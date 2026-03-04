# 🧠 COGNIVITA – Intelligent Alzheimer Detection & Monitoring System

> AI-powered medical platform for early Alzheimer's detection using MRI brain scans and cognitive assessment (MMSE test).

This project was developed as part of the **PIDEV – 3rd Year Engineering Program** at **Esprit School of Engineering** (Academic Year 2025–2026).

![Angular](https://img.shields.io/badge/Angular-17-red?style=flat-square&logo=angular)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3-green?style=flat-square&logo=springboot)
![Python](https://img.shields.io/badge/Python-3.10-blue?style=flat-square&logo=python)
![TensorFlow](https://img.shields.io/badge/TensorFlow-2.x-orange?style=flat-square&logo=tensorflow)
![Docker](https://img.shields.io/badge/Docker-Containerized-blue?style=flat-square&logo=docker)

---

## 📌 Overview

Alzheimer's disease is a progressive neurodegenerative disorder that requires early detection for better patient management. **COGNIVITA** combines cutting-edge AI with a robust clinical platform to support medical professionals in early diagnosis and monitoring.

The system integrates:
- 🧠 **MRI Image Classification** using CNN and Transfer Learning
- 📝 **Automated MMSE Cognitive Assessment**
- 📊 **Clinical Dashboard** for monitoring disease progression
- 🔐 **Secure Microservices Architecture** with JWT & OAuth2

---

## 🎯 Business Objectives

| ID  | Objective |
|-----|-----------|
| BO1 | Early detection of Alzheimer's disease from MRI scans |
| BO2 | Automate cognitive evaluation using the MMSE test |
| BO3 | Reduce medical diagnosis time significantly |
| BO4 | Provide intelligent progression alerts for clinicians |
| BO5 | Centralize patient results in a unified clinical dashboard |

---

## 🔬 Data Science Objectives

| ID   | Objective |
|------|-----------|
| DSO1 | Build MRI classification model (Normal / Early Stage / Alzheimer) |
| DSO2 | Achieve accuracy above 85% on test data |
| DSO3 | Identify discriminative MRI patterns using explainability tools |
| DSO4 | Analyze cognitive progression through MMSE score tracking |
| DSO5 | Reduce overfitting via regularization and data augmentation |

---

## 📊 Dataset

| Source | Description |
|--------|-------------|
| Public Alzheimer MRI Dataset | Brain scan images (4 severity classes) |
| MMSE Cognitive Scores | Patient cognitive assessment results |
| Patient Metadata | Age, gender, clinical history |

---

## 🔎 Data Processing

### Data Cleaning
- Remove corrupted or low-quality MRI images
- Handle missing values in patient metadata
- Normalize pixel values to range [0–1]
- Resize all images to **224×224** pixels
- One-hot encode classification labels

### Data Augmentation
- Random rotation, flipping, zoom
- Brightness and contrast adjustment
- Prevents overfitting on small medical datasets

### PCA Analysis
- Dimensionality reduction applied to feature space
- First 2 components explain ~70% of variance
- **PC1** → Global cognitive severity
- **PC2** → Memory decline patterns

---

## 🤖 Models Implemented

| Model | Type | Purpose |
|-------|------|---------|
| CNN (Custom) | Deep Learning | MRI image classification |
| MobileNet | Transfer Learning | Lightweight, high accuracy |
| ResNet50 | Transfer Learning | Deep feature extraction |
| Logistic Regression | Baseline | Binary classification |
| Random Forest | Comparison | Feature importance analysis |

### ✅ Final Model: CNN + Transfer Learning

Chosen for:
- Superior performance on medical image data
- Automatic feature extraction from MRI scans
- Strong generalization capability
- High sensitivity for Alzheimer detection (low false-negative rate)

---

## 📈 Performance Metrics

| Metric | Score |
|--------|-------|
| Accuracy | 87–92% |
| Precision | High |
| Recall | High (priority for medical use) |
| F1-Score | Balanced |
| AUC-ROC | > 0.90 |

> ⚠️ Low false-negative rate prioritized — missing an Alzheimer diagnosis is clinically critical.

---

## 🖥️ Features

- 🧠 **MRI Upload & AI Prediction** — Upload brain scan, get instant classification
- 📝 **Digital MMSE Test** — Automated cognitive scoring
- 📊 **Progression Monitoring** — Track patient evolution over time
- 🔔 **Intelligent Alert System** — Notify clinicians of critical changes
- 🏥 **Clinical Dashboard** — Centralized patient data view
- 🌐 **3D Brain Visualization** — Interactive MRI rendering
- 🔐 **OAuth2 Login** — Google & Facebook authentication
- 📧 **2FA Email OTP** — Security verification every 3 days
- 👤 **Role-Based Access** — Admin / Doctor / Patient roles

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────┐
│                   Angular 17 Frontend                │
│              (Port 4200 – Cyberpunk UI)              │
└───────────────────────┬─────────────────────────────┘
                        │ HTTP / REST
┌───────────────────────▼─────────────────────────────┐
│                    API Gateway                       │
│               (Spring Cloud Gateway)                 │
└──────┬──────────────┬──────────────┬────────────────┘
       │              │              │
┌──────▼──────┐ ┌─────▼──────┐ ┌────▼───────────────┐
│ user-service│ │ mri-service│ │   ai-service        │
│ (Port 8081) │ │ (Port 8082)│ │ (Python / Flask)    │
│ Spring Boot │ │Spring Boot │ │ TensorFlow / CNN    │
└──────┬──────┘ └─────┬──────┘ └────────────────────┘
       │              │
┌──────▼──────────────▼──────────────────────────────┐
│                  MySQL Database                      │
└─────────────────────────────────────────────────────┘
       │
┌──────▼──────────────────────────────────────────────┐
│              Service Discovery (Eureka)              │
│              Config Server (Spring Cloud)            │
└─────────────────────────────────────────────────────┘
```

---

## 🛠️ Tech Stack

### Frontend
| Technology | Version | Purpose |
|-----------|---------|---------|
| Angular | 17 | SPA Framework |
| TypeScript | 5.x | Language |
| Reactive Forms | — | Form handling |
| CSS3 | — | Cyberpunk UI design |

### Backend
| Technology | Version | Purpose |
|-----------|---------|---------|
| Java | 17 | Language |
| Spring Boot | 3.x | Microservices framework |
| Spring Security | 6.x | Auth & OAuth2 |
| Spring Cloud | — | Service discovery & config |
| JWT | — | Token-based authentication |
| Hibernate / JPA | — | ORM |
| MySQL | 8 | Database |

### AI / Machine Learning
| Technology | Purpose |
|-----------|---------|
| Python 3.10 | AI language |
| TensorFlow / Keras | Deep learning framework |
| CNN | MRI image classification |
| MobileNet / ResNet | Transfer learning |
| Flask | AI microservice API |
| OpenCV | Image preprocessing |
| NumPy / Pandas | Data processing |
| Matplotlib / Seaborn | Visualization |
| Scikit-learn | Baseline models |

### DevOps & Infrastructure
| Technology | Purpose |
|-----------|---------|
| Docker | Containerization |
| Kubernetes | Orchestration |
| Jenkins | CI/CD Pipeline |
| Eureka | Service discovery |
| GitHub | Version control |

---

## 👥 Contributors

| Name | Role |
|------|------|
| Ali Najjaa | Full Stack Developer |
| [Coéquipier 2] | [Rôle] |
| [Coéquipier 3] | [Rôle] |
| [Coéquipier 4] | [Rôle] |

---

## 🎓 Academic Context

Developed at **Esprit School of Engineering – Tunisia**
**PIDEV – 3rd Year Engineering | Academic Year 2025–2026**

---

## 🚀 Getting Started

### Prerequisites

```bash
Node.js 18+
Java 17+
MySQL 8+
Maven 3.8+
Python 3.10+
Docker (optional)
```

### 1. Clone the repository

```bash
git clone https://github.com/TON_USERNAME/Esprit-PIDEV-3AXX-2026-Cognivita.git
cd Esprit-PIDEV-3AXX-2026-Cognivita
```

### 2. Backend — Spring Boot

```bash
# Config Server (start first)
cd config-server
mvn spring-boot:run

# User Service
cd user-service1
mvn spring-boot:run
```

### 3. Frontend — Angular

```bash
cd frontend
npm install
ng serve
# → http://localhost:4200
```

### 4. AI Service — Python / Flask

```bash
cd ai-service
pip install -r requirements.txt
python app.py
# → http://localhost:5000
```

### 5. Environment Variables

Configure in `application.properties`:

```properties
spring.mail.username=YOUR_EMAIL
spring.mail.password=YOUR_APP_PASSWORD
spring.security.oauth2.client.registration.google.client-id=YOUR_ID
spring.security.oauth2.client.registration.google.client-secret=YOUR_SECRET
spring.security.oauth2.client.registration.facebook.client-id=YOUR_ID
spring.security.oauth2.client.registration.facebook.client-secret=YOUR_SECRET
```

### 6. Docker (optional)

```bash
docker-compose up --build
```

---

## 📁 Project Structure

```
Cognivita/
├── config-server/          # Spring Cloud Config
├── eureka-server/          # Service Discovery
├── api-gateway/            # Spring Cloud Gateway
├── user-service1/          # Auth, Users, 2FA
├── mri-service/            # MRI upload & management
├── ai-service/             # Python CNN model (Flask)
│   ├── model/              # Trained CNN weights
│   ├── preprocessing/      # Image pipeline
│   └── app.py              # Flask API
└── frontend/               # Angular 17 app
    └── src/app/
        ├── FrontOffice/    # Patient views
        └── BackOffice/     # Admin dashboard
```

---

## 🙏 Acknowledgments

- **Esprit School of Engineering** – Tunisia
- Encadrant : [Nom du tuteur]
- Dataset : Public Alzheimer MRI Dataset (Kaggle)
- Année universitaire : **2025–2026**

---

*Developed with ❤️ at Esprit School of Engineering – Tunisia*
