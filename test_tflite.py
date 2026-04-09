import tensorflow as tf
interpreter = tf.lite.Interpreter(model_path="app/src/main/assets/yamnet.tflite")
for det in interpreter.get_output_details():
    print(det['shape'])
