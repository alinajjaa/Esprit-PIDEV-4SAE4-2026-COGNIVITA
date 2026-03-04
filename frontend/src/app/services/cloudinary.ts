import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class CloudinaryService {

  private cloudName = 'db5fae4od';  // Remplacez par votre Cloud Name trouvé sur Cloudinary
  private uploadPreset = 'profile_pictures';  // Remplacez par le nom de votre Upload Preset

  constructor(private http: HttpClient) {}

  // Méthode pour uploader l'image sur Cloudinary
  uploadImage(file: File): Observable<any> {
    const formData = new FormData();
    formData.append('file', file);  // Ajouter le fichier sélectionné
    formData.append('upload_preset', this.uploadPreset);  // Nom de votre upload preset

    // URL pour l'API Cloudinary
    const url = `https://api.cloudinary.com/v1_1/${this.cloudName}/image/upload`;

    return this.http.post<any>(url, formData);
  }
}