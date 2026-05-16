# WorkHub — Deployment Guide

Covers three deployment tracks:

1. **Local** — `docker compose` (full stack on your laptop)
2. **Kubernetes** — local cluster via Minikube or Kind
3. **Free Cloud** — AWS Free Tier via Terraform (EKS track)

---

## Prerequisites (all tracks)

| Tool | Minimum version | Install |
|------|-----------------|---------|
| Java | 21 | <https://adoptium.net> |
| Maven | 3.9+ | <https://maven.apache.org/download.cgi> |
| Docker Desktop | 4.x | <https://docs.docker.com/get-docker/> |
| Git | any | <https://git-scm.com> |

```bash
git clone https://github.com/Rehab-M-Esmail/workhub.git
cd workhub
```

---

## Track 1 — Local Docker Compose

### What starts

| Container | Port (host) | Purpose |
|-----------|-------------|---------|
| `workhub-postgres` | 5433 | PostgreSQL 16 database |
| `workhub-redpanda` | 9092 | Kafka-compatible broker |
| `workhub-app` | 8080 | Spring Boot API |
| `prometheus` | 9090 | Metrics scraper |
| `grafana` | 3000 | Dashboards |

### Start

```bash
docker compose up --build
```

The compose file applies health-check dependencies so the app only starts after Postgres and Redpanda are healthy. First build takes ~2 minutes; subsequent runs use the layer cache.

### Verify

```bash
# API health
curl http://localhost:8080/actuator/health

# Swagger UI
open http://localhost:8080/swagger-ui.html


# Grafana (anonymous admin — see grafana.ini)
open http://localhost:3000
```

### Typical first-run flow

```bash
# 1. Create a tenant
curl -s -X POST http://localhost:8080/tenant \
  -H "Content-Type: application/json" \
  -d '{"name":"Acme","plan":"FREE"}' | jq .

# 2. Register a user (note the returned JWT)
curl -s -X POST http://localhost:8080/user/signup \
  -H "Content-Type: application/json" \
  -d '{"email":"alice@acme.com","password":"secret","role":"TENANT_ADMIN","tenantName":"Acme"}' | jq .

# 3. Login
TOKEN=$(curl -s -X POST http://localhost:8080/user/login \
  -H "Content-Type: application/json" \
  -d '{"tenantName":"Acme","email":"alice@acme.com","password":"secret"}' | jq -r '.token')

# 4. Create a project (replace 1 with your tenant ID)
curl -s -X POST http://localhost:8080/projects \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-ID: 1" \
  -H "Content-Type: application/json" \
  -d '{"projectName":"Alpha","initialTaskTitle":"Setup CI","simulateTaskFailure":false}' | jq .

# 5. Enqueue an async report
curl -s -X POST http://localhost:8080/reports \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-ID: 1" \
  -H "Content-Type: application/json" \
  -d '{"reportType":"TENANT_ACTIVITY","tenantId":1}' | jq .
```

### Stop and clean up

```bash
docker compose down -v          # -v also removes named volumes (DB data)
```

---

## Track 2 — Kubernetes (local cluster)

### Requirements (in addition to prerequisites)

| Tool | Install |
|------|---------|
| kubectl | <https://kubernetes.io/docs/tasks/tools/> |
| Minikube **or** Kind | <https://minikube.sigs.k8s.io> / <https://kind.sigs.k8s.io> |

### 2a — Start local cluster

**Minikube**

```bash
minikube start --cpus=4 --memory=6g
eval $(minikube docker-env)          # point Docker CLI at Minikube's daemon
```

**Kind**

```bash
kind create cluster --name workhub
kubectl cluster-info --context kind-workhub
```

### 2b — Build and load the image

```bash
mvn -q -DskipTests package

# Minikube (image goes straight into cluster)
# The manifest (k8s/springboot.yaml) uses image: workhub-workhub-app:latest
# with imagePullPolicy: Never — the tag must match exactly.
docker build -t workhub-workhub-app:latest .

# Kind — also requires explicitly loading the image into the cluster
docker build -t workhub-workhub-app:latest .
kind load docker-image workhub-workhub-app:latest --name workhub

# NOTE: for GHCR (CI-built image) the full package path is:
#   ghcr.io/workhub/workhub-app:<sha>
# To use it in Kubernetes, update the image field in k8s/springboot.yaml
# and set imagePullPolicy: Always (or IfNotPresent with a pull secret).
```

### 2c — Apply manifests

```bash
# Secrets first
kubectl apply -f k8s/secret.yaml

# Infrastructure
kubectl apply -f k8s/postgres.yaml
kubectl apply -f k8s/redpanda.yaml

# Wait for infrastructure to be ready
kubectl wait --for=condition=ready pod -l app=postgres   --timeout=120s
kubectl wait --for=condition=ready pod -l app=redpanda   --timeout=120s

# ConfigMap, then application
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/springboot.yaml

# Observability
kubectl apply -f k8s/prometheus-config.yaml
kubectl apply -f k8s/prometheus.yaml
kubectl apply -f k8s/grafana-datasources-configmap.yaml
kubectl apply -f k8s/grafana.yaml
```

### 2d — Verify

```bash
kubectl get pods
kubectl get services
```

Expected pods (all `Running`):

```
springboot-app-*   1/1   Running
postgres-*         1/1   Running
redpanda-*         1/1   Running
prometheus-*       1/1   Running
grafana-*          1/1   Running
```

### 2e — Access the app

**Minikube**

```bash
minikube service springboot-service --url
# Opens http://<minikube-ip>:30080
```

**Kind / any cluster without LoadBalancer**

```bash
kubectl port-forward svc/springboot-service 8080:80 &
# API now at http://localhost:8080
```

Other NodePort services:

| Service | NodePort | Port-forward alternative |
|---------|----------|--------------------------|
| Prometheus | 30090 | `kubectl port-forward svc/prometheus-service 9090:9090` |
| Grafana | 30030 | `kubectl port-forward svc/grafana-service 3000:3000` |

### 2f — Liveness and readiness probes

The Spring Boot deployment (`k8s/springboot.yaml`) exposes:

- **Liveness**: `GET /actuator/health/liveness` — initial delay 60 s, period 10 s
- **Readiness**: `GET /actuator/health/readiness` — initial delay 40 s, period 5 s

Kubernetes will automatically restart the container if liveness fails and hold traffic back if readiness fails.

Check probe status:

```bash
kubectl describe pod -l app=springboot-app | grep -A5 Liveness
```

### 2g — Tear down

```bash
kubectl delete -f k8s/

# Minikube
minikube stop

# Kind
kind delete cluster --name workhub
```

---

## Track 3 — Free Cloud Deployment (AWS Free Tier via Terraform)

> **Free-tier note:** EKS control plane costs $0.10/hr and is **not** free-tier eligible.
> Use the **EC2 + Docker Compose track** below for a truly zero-cost cloud deployment.

### Option A — EC2 + Docker Compose (recommended free option)

This spins up a single `t2.micro` EC2 instance (750 free-tier hours/month), copies the repo, and runs `docker compose up`. It mirrors the local compose stack exactly.

#### Prerequisites

| Tool | Install |
|------|---------|
| Terraform ≥ 1.5 | <https://developer.hashicorp.com/terraform/install> |
| AWS CLI v2 | <https://docs.aws.amazon.com/cli/latest/userguide/install-cliv2.html> |
| An AWS account | <https://aws.amazon.com/free/> |

```bash
aws configure          # enter Access Key ID, Secret, region (e.g. us-east-1), output json
aws sts get-caller-identity   # confirm credentials work
```

#### Terraform setup

The repo's `terraform/` folder provisions full EKS + RDS + MSK (non-free). For the free EC2 option, use the inline configuration below instead.

Create a new directory alongside the repo and save the following as `main.tf`:

```hcl
# free-deploy/main.tf
terraform {
  required_providers {
    aws = { source = "hashicorp/aws", version = "~> 5.0" }
  }
}

provider "aws" { region = var.aws_region }

variable "aws_region"   { default = "us-east-1" }
variable "key_pair_name" {
  description = "Name of an existing EC2 key pair for SSH access"
}

# Use the default VPC so no networking resources are needed
data "aws_vpc" "default" { default = true }
data "aws_subnets" "default" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.default.id]
  }
}

# Latest Amazon Linux 2023 AMI
data "aws_ami" "al2023" {
  most_recent = true
  owners      = ["amazon"]
  filter {
    name   = "name"
    values = ["al2023-ami-*-x86_64"]
  }
}

resource "aws_security_group" "workhub" {
  name        = "workhub-free-sg"
  description = "WorkHub EC2 security group"
  vpc_id      = data.aws_vpc.default.id

  ingress { from_port = 22;   to_port = 22;   protocol = "tcp"; cidr_blocks = ["0.0.0.0/0"] }
  ingress { from_port = 8080; to_port = 8080; protocol = "tcp"; cidr_blocks = ["0.0.0.0/0"] }
  ingress { from_port = 9090; to_port = 9090; protocol = "tcp"; cidr_blocks = ["0.0.0.0/0"] }
  ingress { from_port = 3000; to_port = 3000; protocol = "tcp"; cidr_blocks = ["0.0.0.0/0"] }
  egress  { from_port = 0;    to_port = 0;    protocol = "-1";  cidr_blocks = ["0.0.0.0/0"] }
}

resource "aws_instance" "workhub" {
  ami                    = data.aws_ami.al2023.id
  instance_type          = "t2.micro"            # free-tier eligible
  key_name               = var.key_pair_name
  subnet_id              = tolist(data.aws_subnets.default.ids)[0]
  vpc_security_group_ids = [aws_security_group.workhub.id]

  user_data = <<-EOF
    #!/bin/bash
    set -e
    dnf install -y docker git
    systemctl enable --now docker
    usermod -aG docker ec2-user
    curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-linux-x86_64" \
      -o /usr/local/bin/docker-compose
    chmod +x /usr/local/bin/docker-compose
    git clone https://github.com/Rehab-M-Esmail/workhub.git /home/ec2-user/workhub
    chown -R ec2-user:ec2-user /home/ec2-user/workhub
    cd /home/ec2-user/workhub
    docker-compose up --build -d
  EOF

  tags = { Name = "workhub-free-tier" }
}

output "app_url"        { value = "http://${aws_instance.workhub.public_ip}:8080" }
output "prometheus_url" { value = "http://${aws_instance.workhub.public_ip}:9090" }
output "grafana_url"    { value = "http://${aws_instance.workhub.public_ip}:3000" }
output "ssh_command"    { value = "ssh ec2-user@${aws_instance.workhub.public_ip}" }
```

#### Deploy

```bash
cd free-deploy

# You need an existing EC2 key pair — create one in the AWS console if you don't have one
terraform init
terraform apply -var="key_pair_name=YOUR_KEY_PAIR_NAME"
```

Terraform will output the public IP. Wait ~3 minutes for `user_data` to finish bootstrapping.

```bash
# Check the app (expect {"status":"UP"})
curl http://<PUBLIC_IP>:8080/actuator/health

# Open Swagger
open http://<PUBLIC_IP>:8080/swagger-ui.html
```

#### Verify cloud health checks

```bash
# Liveness
curl http://<PUBLIC_IP>:8080/actuator/health/liveness

# Readiness
curl http://<PUBLIC_IP>:8080/actuator/health/readiness

# Prometheus metrics
curl http://<PUBLIC_IP>:8080/actuator/prometheus | head -20
```

#### Tear down (avoid charges)

```bash
terraform destroy -var="key_pair_name=YOUR_KEY_PAIR_NAME"
```

---

### Option B — Full EKS + RDS + MSK (Terraform in `terraform/`)

> **Cost warning:** EKS, RDS, and MSK are **not** free-tier eligible. Expect ~$5–15/day.
> Always run `terraform destroy` when done.

```bash
cd terraform
cp terraform.tfvars.example terraform.tfvars
# Edit terraform.tfvars: set db_username, db_password, aws_region

terraform init
terraform validate        # must print: Success! The configuration is valid.
terraform plan -out=tfplan
terraform apply tfplan
```

After apply, configure kubectl and deploy the app manifests:

```bash
# Configure kubectl
$(terraform output -raw kubeconfig_command)

# Apply Kubernetes manifests (update image to ECR URL first)
ECR_URL=$(terraform output -raw ecr_repository_url)
# ECR URL will be in the form: <account>.dkr.ecr.<region>.amazonaws.com/workhub-app
docker build -t $ECR_URL:latest .
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin $ECR_URL
docker push $ECR_URL:latest

# Update k8s/springboot.yaml — replace the image line:
#   image: workhub-workhub-app:latest   ← local/Minikube tag
# with:
#   image: <ECR_URL>:latest             ← ECR tag
# then remove or change imagePullPolicy to Always before applying.

kubectl apply -f k8s/secret.yaml
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/springboot.yaml
kubectl apply -f k8s/prometheus-config.yaml
kubectl apply -f k8s/prometheus.yaml
kubectl apply -f k8s/grafana-datasources-configmap.yaml
kubectl apply -f k8s/grafana.yaml
```

Key Terraform outputs:

| Output | Use |
|--------|-----|
| `rds_endpoint` | Set as `DB_HOST` in ConfigMap |
| `msk_bootstrap_brokers` | Set as `KAFKA_BOOTSTRAP_SERVERS` |
| `ecr_repository_url` | Image tag for `k8s/springboot.yaml` |
| `kubeconfig_command` | Connects `kubectl` to the cluster |

Tear down:

```bash
kubectl delete -f k8s/
terraform destroy
```

---

## Environment variables reference

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_HOST` | `localhost` | PostgreSQL hostname |
| `DB_USERNAME` | `shahd` | DB user |
| `DB_PASSWORD` | `password` | DB password |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Kafka/Redpanda broker |

All three tracks inject these differently: compose uses `environment:` blocks, Kubernetes uses a Secret + ConfigMap, and the EC2 track uses `docker compose` with the same defaults as local.

---

## CI/CD pipeline

The GitHub Actions workflow at `.github/workflows/ci.yml` runs automatically on every push and pull request to `main`:

1. **Test job** — spins up a Postgres service container, runs `mvn test`
2. **Build job** (after tests pass) — builds the JAR, then builds and pushes the Docker image to GHCR (`ghcr.io/workhub/workhub-app:<sha>`)
