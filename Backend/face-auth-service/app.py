from flask import Flask
from flask_cors import CORS
from routes.register import register_bp
from routes.verify import verify_bp
import os
from dotenv import load_dotenv

# Charger les variables d'environnement depuis .env
load_dotenv()

app = Flask(__name__)

# Configuration depuis .env
PORT = int(os.getenv("FLASK_PORT", 5001))
HOST = os.getenv("FLASK_HOST", "0.0.0.0")
DEBUG = os.getenv("FLASK_DEBUG", "False").lower() == "true"
CORS_ORIGINS = os.getenv("CORS_ORIGINS", "*").split(",")

# CORS avec origines configurables (plus sécurisé que "*")
CORS(app, resources={
    r"/*": {
        "origins": CORS_ORIGINS,
        "methods": ["GET", "POST", "PUT", "DELETE", "OPTIONS"],
        "allow_headers": ["Content-Type", "Authorization"]
    }
})

# Enregistrer les routes
app.register_blueprint(register_bp, url_prefix='/api/face')
app.register_blueprint(verify_bp, url_prefix='/api/face')

@app.route('/health', methods=['GET'])
def health_check():
    return {
        'status': 'running',
        'service': 'Face Auth Service',
        'version': '1.0.0'
    }, 200

if __name__ == '__main__':
    app.run(
        host=HOST,
        port=PORT,
        debug=DEBUG,
        use_reloader=False
    )