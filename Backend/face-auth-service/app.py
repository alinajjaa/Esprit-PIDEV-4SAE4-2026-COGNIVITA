from flask import Flask
from flask_cors import CORS
from routes.register import register_bp
from routes.verify import verify_bp

app = Flask(__name__)

# Autoriser les requêtes depuis Angular (localhost:4200)
CORS(app, resources={
    r"/*": {
        "origins": "*",
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
        host='0.0.0.0',
        port=5001,
        debug=False,        # ✅ désactiver
        use_reloader=False  # ✅ désactiver
    )