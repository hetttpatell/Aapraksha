const { onDocumentUpdated } = require("firebase-functions/v2/firestore");
const { getFirestore } = require("firebase-admin/firestore");
const { GoogleGenAI } = require("@google/genai");

exports.generateIncidentReport = onDocumentUpdated("alerts/{alertId}", async (event) => {
    const newValue = event.data.after.data();
    const previousValue = event.data.before.data();

    // Only run when status changes to RESOLVED (or resolved = true boolean)
    if (!newValue.resolved || previousValue.resolved) {
        return null;
    }

    const { alertId } = event.params;
    const db = getFirestore();

    try {
        // 1. Gather all SOS data
        const durationSec = newValue.durationSeconds || 0;
        const timeOfDay = newValue.timeOfDay || "Unknown";
        const loc = newValue.location || {};
        const finalLat = Number.isFinite(loc.latitude) ? loc.latitude : 0;
        const finalLng = Number.isFinite(loc.longitude) ? loc.longitude : 0;
        const finalAddress = loc.address || "Unknown";
        const locationStr = `Address: ${finalAddress}, Lat: ${finalLat}, Lng: ${finalLng}`;
        const geohash = newValue.geohash || 'Unknown';
        const contactCount = Array.isArray(newValue.notificationsToContacts) ? newValue.notificationsToContacts.length : 0;
        const alertMessage = newValue.alertMessage || "N/A";
        const audioStatus = newValue.audioData && newValue.audioData.status ? newValue.audioData.status : "N/A";
        
        let dangerContext = "Unknown";
        if (geohash !== 'Unknown') {
            const dangerDoc = await db.collection("danger_zones").doc(geohash).get();
            if (dangerDoc.exists) {
                dangerContext = `Danger Rank ${dangerDoc.data().rank}/5 (${dangerDoc.data().label})`;
            }
        }

        // 2. Call Gemini
        const geminiApiKey = process.env.GEMINI_API_KEY;
        if (!geminiApiKey) {
            throw new Error("Missing GEMINI_API_KEY in Cloud Functions environment.");
        }
        const ai = new GoogleGenAI({ apiKey: geminiApiKey });

        const prompt = `You are an AI generating an official SOS Incident Report for Aapraksha Safety App.
Please generate a formal, concise text report based on the following data:
- Alert ID: ${alertId}
- Final Location: ${locationStr}
- Time of Day: ${timeOfDay}
- Duration: ${durationSec} seconds
- Area Danger Context: ${dangerContext}
- Contacts Notified: ${contactCount}
- Smart Message Sent: ${alertMessage}
- SOS Audio Status: ${audioStatus}

Format as a clear, professional summary with bullet points. It should be easily readable when converted to a PDF by the client app. DO NOT use markdown format, just plain text with tabs/newlines.`;

        const response = await ai.models.generateContent({
            model: "gemini-2.5-flash",
            contents: prompt,
        });

        const reportText = response.text.trim();

        // 3. Store report text
        await event.data.after.ref.update({
            incidentReport: reportText
        });

        console.log(`Incident Report generated and saved for alert ${alertId}`);
        return null;

    } catch (error) {
        console.error("Error generating incident report:", error);
        return null;
    }
});
