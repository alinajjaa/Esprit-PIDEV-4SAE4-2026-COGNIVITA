from flask import Blueprint, request, jsonify
import numpy as np
import json
from db.database import get_connection, close_connection

register_bp = Blueprint('register', __name__)

@register_bp.route('/register', methods=['POST'])
def register_face():
    try:
        print("✅ register_face appelé")  # ✅ AJOUTER
        data = request.get_json()
        print(f"✅ data reçue: {data}")   # ✅ AJOUTER

        if not data or 'user_id' not in data or 'embedding' not in data:
            return jsonify({'success': False, 'message': 'user_id et embedding sont requis'}), 400

        user_id = data['user_id']
        embedding = data['embedding']
        print(f"✅ user_id: {user_id}, embedding length: {len(embedding)}")  # ✅ AJOUTER

        embedding_json = json.dumps(embedding)

        connection = get_connection()
        print(f"✅ connection: {connection}")  # ✅ AJOUTER
        
        if not connection:
            return jsonify({'success': False, 'message': 'DB connection failed'}), 500

        cursor = connection.cursor()
        cursor.execute("SELECT id FROM face_embeddings WHERE user_id = %s", (user_id,))
        existing = cursor.fetchone()
        print(f"✅ existing: {existing}")  # ✅ AJOUTER

        if existing:
            cursor.execute("UPDATE face_embeddings SET embedding = %s WHERE user_id = %s", (embedding_json, user_id))
        else:
            cursor.execute("INSERT INTO face_embeddings (user_id, embedding) VALUES (%s, %s)", (user_id, embedding_json))

        connection.commit()
        close_connection(connection, cursor)
        print("✅ succès")  # ✅ AJOUTER

        return jsonify({'success': True, 'message': 'OK', 'user_id': user_id}), 200

    except Exception as e:
        print(f"❌ ERREUR: {str(e)}")  # ✅ AJOUTER
        import traceback
        traceback.print_exc()          # ✅ AJOUTER
        return jsonify({'success': False, 'message': str(e)}), 500