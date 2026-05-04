/**
 * Ports backend (local, START_PROJECT.bat) — pour référence :
 * - 8761  Eureka Server
 * - 9090  API Gateway  ← tous les appels front passent ici pour les microservices
 * - 8080  Backend monolith
 * - 8081  Medical Records | 8082 Health Prevention | 8083 Family Tree
 * - 8084  User | 8085 MMSE | 8086 Medication Adherence | 8087 Activities + Journal | 8088 Notification
 * - 8091  Rendez-vous (π) — context /rdv
 * - 8092  Plan Suivi — context /plan-suivi
 * - 4200  Angular (npm start)
 *
 * Modifie uniquement GATEWAY_ORIGIN ci-dessous si ton gateway tourne sur un autre port.
 */

/** API Gateway Spring Cloud — changer ici uniquement pour tout le frontend */
export const GATEWAY_ORIGIN = 'http://localhost:9090';

/** Backend monolithe (CNN / routes non migrées si utilisées telles quelles) */
export const BACKEND_ORIGIN = 'http://localhost:8080';

/** Serveur CNN Python (optionnel — voir medical-records cnn config) */
export const CNN_PYTHON_ORIGIN = 'http://localhost:8000';

export const API = {
  GATEWAY_ORIGIN,

  RDV_BASE: `${GATEWAY_ORIGIN}/rdv`,
  RDV_API: `${GATEWAY_ORIGIN}/rdv/api/rendezvous`,
  MEDECIN_API: `${GATEWAY_ORIGIN}/rdv/api/medecins`,
  OPTIONS_API: `${GATEWAY_ORIGIN}/rdv/api/options`,
  PATIENTS_API: `${GATEWAY_ORIGIN}/rdv/api/patients`,

  SUIVI_API: `${GATEWAY_ORIGIN}/plan-suivi/api/suivi`,
  DOCTORS_API: `${GATEWAY_ORIGIN}/rdv/api/doctors`,

  ACTIVITIES_API: `${GATEWAY_ORIGIN}/api/activities`,
  JOURNAL_API: `${GATEWAY_ORIGIN}/api/journal`,
};
