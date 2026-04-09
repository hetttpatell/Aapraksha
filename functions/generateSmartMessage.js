const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { getFirestore, Timestamp } = require("firebase-admin/firestore");
const { GoogleGenAI } = require("@google/genai");

exports.generateSmartMessage = onCall(async (request) => {
    // 1. Validate inputs
    const { alertId, name, locationStr, geohash, timeOfDay } = request.data;
    const uid = request.auth ? request.auth.uid : null;

    if (!alertId || !geohash) {
        throw new HttpsError("invalid-argument", "alertId and geohash are required.");
    }
    if (!uid) {
        throw new HttpsError("unauthenticated", "Authentication is required.");
    }

    const db = getFirestore();
    const alertRef = db.collection("alerts").doc(alertId);
    const alertDoc = await alertRef.get();
    if (!alertDoc.exists) {
        throw new HttpsError("not-found", "Alert not found.");
    }
    if (alertDoc.data().userId !== uid) {
        throw new HttpsError("permission-denied", "You can only generate messages for your own alerts.");
    }
    const templateRef = db.collection("message_templates").doc(geohash);

    // 2. Check Cache
    try {
        const templateDoc = await templateRef.get();
        if (templateDoc.exists) {
            const data = templateDoc.data();
            const ageMs = Date.now() - data.generatedAt.toMillis();
            
            // 6 hours cache
            if (ageMs < 6 * 60 * 60 * 1000) {
                // Return cached and personalized template
                const personalizedMsg = personalizeTemplate(data.template, name, locationStr, timeOfDay);
                
                // Update the alert synchronously
                await alertRef.update({ smartMessage: personalizedMsg });

                return { message: personalizedMsg, source: "cache" };
            }
        }

        // 3. Obtain Danger Zone Context if available
        let zoneContext = "";
        const dangerDoc = await db.collection("danger_zones").doc(geohash).get();
        if (dangerDoc.exists && dangerDoc.data().rank > 2) {
            zoneContext = `This area currently has a danger ranking of ${dangerDoc.data().rank}/5 (${dangerDoc.data().label}).`;
        }

        // 4. Call Gemini if not cached or expired
        const geminiApiKey = process.env.GEMINI_API_KEY;
        if (!geminiApiKey) {
            throw new Error("Missing GEMINI_API_KEY in Cloud Functions environment.");
        }
        const ai = new GoogleGenAI({ apiKey: geminiApiKey });
        
        const prompt = `You are a safety assistant for the Aapraksha app.
An SOS alert has been triggered. Create a short, urgent SMS template. 
Use EXACTLY these placeholders (do not replace them with real data): {name}, {location}, {time}.
Context: ${zoneContext}
The message must be less than 120 characters and sound urgent but clear.
Example: "{name} triggered an SOS at {location} during the {time}. Area may be unsafe. Please check on them immediately!"`;

        const response = await ai.models.generateContent({
            model: "gemini-2.5-flash",
            contents: prompt,
        });
        
        let newTemplate = response.text.trim();

        // Ensure it has placeholders, if AI failed
        if (!newTemplate.includes("{name}")) {
             newTemplate = "{name} requires emergency assistance at {location} ({time}). " + zoneContext;
        }

        // 5. Cache the new template
        await templateRef.set({
            template: newTemplate,
            generatedAt: Timestamp.now()
        });

        // 6. Personalize and save to alert
        const personalizedMsg = personalizeTemplate(newTemplate, name, locationStr, timeOfDay);
        await alertRef.set({ smartMessage: personalizedMsg }, { merge: true });

        return { message: personalizedMsg, source: "gemini" };

    } catch (error) {
        console.error("Error in generateSmartMessage:", error);
        
        // Fallback static message
        const fallback = `${name || "The user"} requires emergency assistance at ${locationStr || "their current location"}.`;
        return { message: fallback, source: "fallback" };
    }
});

function personalizeTemplate(template, name, locationStr, timeOfDay) {
    if (!template) return "";
    return template
        .replace("{name}", name || "A user")
        .replace("{location}", locationStr || "their current location")
        .replace("{time}", timeOfDay || "moment");
}
