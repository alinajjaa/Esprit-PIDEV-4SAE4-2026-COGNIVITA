from flask import Blueprint, request, jsonify
import numpy as np
import json
from db.database import get_connection, close_connection

verify_bp = Blueprint('verify', __name__)

@verify_bp.route('/verify', methods=['POST'])
def verify_face():
    try:
        data = request.get_json()

        # Vérifier les données reçues
        if not data or 'user_id' not in data or 'embedding' not in data:
            return jsonify({
                'success': False,
                'message': 'user_id et embedding sont requis'
            }), 400

        user_id = data['user_id']
        embedding_input = np.array(data['embedding'])  # embedding reçu d'Angular

        # Connexion MySQL
        connection = get_connection()
        if not connection:
            return jsonify({
                'success': False,
                'message': 'Erreur de connexion à la base de données'
            }), 500

        cursor = connection.cursor()

        # Récupérer l'embedding stocké
        cursor.execute(
            "SELECT embedding FROM face_embeddings WHERE user_id = %s",
            (user_id,)
        )
        result = cursor.fetchone()
        close_connection(connection, cursor)

        if not result:
            return jsonify({
                'success': False,
                'message': 'Aucun visage enregistré pour cet utilisateur'
            }), 404

        # Comparer les embeddings
        embedding_stored = np.array(json.loads(result[0]))

        # Calcul de la distance euclidienne
        distance = np.linalg.norm(embedding_input - embedding_stored)

        # Seuil de similarité (0.6 = valeur optimale)
        threshold = 0.6
        match = bool(distance < threshold)

        return jsonify({
            'success': True,
            'match': match,
            'distance': float(distance),
            'threshold': threshold,
            'message': 'Visage reconnu ✅' if match else 'Visage non reconnu ❌'
        }), 200

    except Exception as e:
        return jsonify({
            'success': False,
            'message': f'Erreur serveur : {str(e)}'
        }), 500
