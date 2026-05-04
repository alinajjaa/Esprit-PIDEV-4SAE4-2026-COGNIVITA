#!/usr/bin/env bash
# =============================================================================
# setup-vm.sh — Bootstrap the Vagrant Ubuntu VM for COGNIVITA PIDEV
# Installs: Docker, Docker Compose, kubectl, kubeadm, kubelet,
#           Java 17, Maven 3, Node.js 20, Angular CLI, SonarScanner CLI
#
# Usage (inside the VM):
#   chmod +x setup-vm.sh
#   ./setup-vm.sh
# =============================================================================

set -euo pipefail

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log()  { echo -e "${GREEN}[SETUP]${NC} $*"; }
warn() { echo -e "${YELLOW}[WARN]${NC}  $*"; }

# ---------------------------------------------------------------------------
# 0. Ensure script runs as a user with sudo (not root directly)
# ---------------------------------------------------------------------------
if [ "$EUID" -eq 0 ]; then
    warn "Running as root. Prefer running as 'vagrant' with sudo privileges."
fi

log "Updating apt package index..."
sudo apt-get update -y
sudo apt-get install -y \
    ca-certificates curl gnupg lsb-release \
    apt-transport-https software-properties-common \
    unzip wget git jq

# ===========================================================================
# 1. DOCKER
# ===========================================================================
log "Installing Docker Engine..."

if command -v docker &>/dev/null; then
    warn "Docker already installed: $(docker --version)"
else
    # Add Docker's official GPG key
    sudo install -m 0755 -d /etc/apt/keyrings
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg \
        | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
    sudo chmod a+r /etc/apt/keyrings/docker.gpg

    # Add the Docker repository
    echo \
        "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] \
        https://download.docker.com/linux/ubuntu \
        $(. /etc/os-release && echo "$VERSION_CODENAME") stable" \
        | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

    sudo apt-get update -y
    sudo apt-get install -y \
        docker-ce docker-ce-cli containerd.io \
        docker-buildx-plugin docker-compose-plugin

    # Allow vagrant user to run docker without sudo
    sudo usermod -aG docker vagrant
    log "Docker installed: $(docker --version)"
fi

# ===========================================================================
# 2. DOCKER COMPOSE (standalone binary — also installed via plugin above)
# ===========================================================================
log "Verifying Docker Compose..."
if docker compose version &>/dev/null; then
    log "Docker Compose (plugin): $(docker compose version)"
else
    warn "Docker Compose plugin not available. Installing standalone..."
    COMPOSE_VERSION="v2.27.0"
    sudo curl -SL \
        "https://github.com/docker/compose/releases/download/${COMPOSE_VERSION}/docker-compose-linux-$(uname -m)" \
        -o /usr/local/bin/docker-compose
    sudo chmod +x /usr/local/bin/docker-compose
    log "Docker Compose standalone: $(docker-compose --version)"
fi

# ===========================================================================
# 3. JAVA 17 (Temurin via Adoptium)
# ===========================================================================
log "Installing Java 17 (Eclipse Temurin)..."

if java -version 2>&1 | grep -q '17\.'; then
    warn "Java 17 already installed."
else
    wget -qO - https://packages.adoptium.net/artifactory/api/gpg/key/public \
        | sudo gpg --dearmor -o /etc/apt/keyrings/adoptium.gpg
    echo "deb [signed-by=/etc/apt/keyrings/adoptium.gpg] \
        https://packages.adoptium.net/artifactory/deb \
        $(awk -F= '/^VERSION_CODENAME/{print $2}' /etc/os-release) main" \
        | sudo tee /etc/apt/sources.list.d/adoptium.list > /dev/null
    sudo apt-get update -y
    sudo apt-get install -y temurin-17-jdk
    log "Java installed: $(java -version 2>&1 | head -1)"
fi

export JAVA_HOME=$(dirname $(dirname $(readlink -f $(which java))))
echo "export JAVA_HOME=${JAVA_HOME}" | sudo tee -a /etc/environment > /dev/null

# ===========================================================================
# 4. MAVEN 3
# ===========================================================================
log "Installing Maven..."

if command -v mvn &>/dev/null; then
    warn "Maven already installed: $(mvn -version | head -1)"
else
    MVN_VERSION="3.9.6"
    wget -q "https://archive.apache.org/dist/maven/maven-3/${MVN_VERSION}/binaries/apache-maven-${MVN_VERSION}-bin.tar.gz" \
        -O /tmp/maven.tar.gz
    sudo tar -xzf /tmp/maven.tar.gz -C /opt
    sudo ln -sf /opt/apache-maven-${MVN_VERSION}/bin/mvn /usr/local/bin/mvn
    rm /tmp/maven.tar.gz
    log "Maven installed: $(mvn -version | head -1)"
fi

# ===========================================================================
# 5. NODE.JS 20 (via NodeSource)
# ===========================================================================
log "Installing Node.js 20..."

if command -v node &>/dev/null && node --version | grep -q '^v20\.'; then
    warn "Node.js 20 already installed: $(node --version)"
else
    curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
    sudo apt-get install -y nodejs
    log "Node.js installed: $(node --version)"
    log "npm installed: $(npm --version)"
fi

# ===========================================================================
# 6. ANGULAR CLI
# ===========================================================================
log "Installing Angular CLI..."

if command -v ng &>/dev/null; then
    warn "Angular CLI already installed: $(ng version 2>/dev/null | head -1)"
else
    sudo npm install -g @angular/cli
    log "Angular CLI installed: $(ng version 2>/dev/null | head -1)"
fi

# ===========================================================================
# 7. KUBECTL
# ===========================================================================
log "Installing kubectl..."

if command -v kubectl &>/dev/null; then
    warn "kubectl already installed: $(kubectl version --client --short 2>/dev/null)"
else
    KUBECTL_VERSION="v1.29.3"
    curl -LO "https://dl.k8s.io/release/${KUBECTL_VERSION}/bin/linux/amd64/kubectl"
    sudo install -o root -g root -m 0755 kubectl /usr/local/bin/kubectl
    rm kubectl
    log "kubectl installed: $(kubectl version --client --short 2>/dev/null)"
fi

# ===========================================================================
# 8. KUBEADM + KUBELET (Kubernetes 1.29)
# ===========================================================================
log "Installing kubeadm and kubelet..."

if command -v kubeadm &>/dev/null; then
    warn "kubeadm already installed: $(kubeadm version -o short 2>/dev/null)"
else
    sudo apt-get install -y apt-transport-https ca-certificates curl gpg

    curl -fsSL https://pkgs.k8s.io/core:/stable:/v1.29/deb/Release.key \
        | sudo gpg --dearmor -o /etc/apt/keyrings/kubernetes-apt-keyring.gpg

    echo 'deb [signed-by=/etc/apt/keyrings/kubernetes-apt-keyring.gpg] \
        https://pkgs.k8s.io/core:/stable:/v1.29/deb/ /' \
        | sudo tee /etc/apt/sources.list.d/kubernetes.list > /dev/null

    sudo apt-get update -y
    sudo apt-get install -y kubelet kubeadm
    sudo apt-mark hold kubelet kubeadm
    log "kubeadm installed: $(kubeadm version -o short 2>/dev/null)"
fi

# ===========================================================================
# 9. SONARSCANNER CLI
# ===========================================================================
log "Installing SonarScanner CLI..."

SONAR_SCANNER_VERSION="6.1.0.4477"
SONAR_SCANNER_DIR="/opt/sonar-scanner"

if [ -x "${SONAR_SCANNER_DIR}/bin/sonar-scanner" ]; then
    warn "SonarScanner already installed at ${SONAR_SCANNER_DIR}"
else
    wget -q \
        "https://binaries.sonarsource.com/Distribution/sonar-scanner-cli/sonar-scanner-cli-${SONAR_SCANNER_VERSION}-linux.zip" \
        -O /tmp/sonar-scanner.zip
    sudo unzip -q /tmp/sonar-scanner.zip -d /opt
    sudo mv /opt/sonar-scanner-${SONAR_SCANNER_VERSION}-linux ${SONAR_SCANNER_DIR}
    sudo ln -sf ${SONAR_SCANNER_DIR}/bin/sonar-scanner /usr/local/bin/sonar-scanner
    rm /tmp/sonar-scanner.zip
    log "SonarScanner installed: $(sonar-scanner --version 2>/dev/null | head -1)"
fi

# ===========================================================================
# 10. START DOCKER SERVICE
# ===========================================================================
log "Enabling and starting Docker service..."
sudo systemctl enable docker
sudo systemctl start docker

# ===========================================================================
# 11. JENKINS (Docker container — shares Docker socket + kubectl)
# ===========================================================================
log "Setting up Jenkins as a Docker container..."

if docker ps -a --format '{{.Names}}' | grep -q '^jenkins$'; then
    warn "Jenkins container already exists. Skipping."
else
    docker volume create jenkins_home

    docker run -d \
        --name jenkins \
        --restart unless-stopped \
        -p 8080:8080 \
        -p 50000:50000 \
        -v jenkins_home:/var/jenkins_home \
        -v /var/run/docker.sock:/var/run/docker.sock \
        -v /usr/local/bin/kubectl:/usr/local/bin/kubectl \
        -v /root/.kube:/root/.kube \
        jenkins/jenkins:lts-jdk17

    # Give Jenkins access to Docker socket
    sudo chmod 666 /var/run/docker.sock

    log "Jenkins started. Initial admin password command:"
    log "  docker exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword"
fi

# ===========================================================================
# 12. SUMMARY
# ===========================================================================
echo ""
echo "======================================================================"
echo " INSTALLATION COMPLETE — Tool versions:"
echo "======================================================================"
echo "  Docker        : $(docker --version 2>/dev/null || echo 'NOT FOUND')"
echo "  Docker Compose: $(docker compose version 2>/dev/null || echo 'NOT FOUND')"
echo "  Java          : $(java -version 2>&1 | head -1)"
echo "  Maven         : $(mvn -version 2>/dev/null | head -1)"
echo "  Node.js       : $(node --version 2>/dev/null || echo 'NOT FOUND')"
echo "  npm           : $(npm --version 2>/dev/null || echo 'NOT FOUND')"
echo "  Angular CLI   : $(ng version 2>/dev/null | grep 'Angular CLI' | head -1 || echo 'NOT FOUND')"
echo "  kubectl       : $(kubectl version --client --short 2>/dev/null || echo 'NOT FOUND')"
echo "  kubeadm       : $(kubeadm version -o short 2>/dev/null || echo 'NOT FOUND')"
echo "  SonarScanner  : $(sonar-scanner --version 2>/dev/null | head -1 || echo 'NOT FOUND')"
echo "  Jenkins       : $(docker inspect --format='{{.State.Status}}' jenkins 2>/dev/null || echo 'NOT RUNNING')"
echo "======================================================================"
echo ""
echo " IMPORTANT: Log out and back in (or run 'newgrp docker') so your"
echo " user can run Docker commands without sudo."
echo ""
echo " Access Jenkins   : http://localhost:8080"
echo " Access SonarQube : http://localhost:9000  (after: cd devops/sonarqube && docker compose up -d)"
echo " Access Grafana   : http://localhost:3000  (after: cd devops/monitoring && docker compose up -d)"
echo "======================================================================"
