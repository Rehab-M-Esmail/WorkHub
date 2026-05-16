# WorkHub — AWS EKS infrastructure (Terraform)

## Overview

This module provisions **WorkHub** infrastructure on **AWS EKS** with the same logical topology as local `docker-compose.yml`, using production-style managed services:

| Local (`docker-compose`) | AWS |
|--------------------------|-----|
| `postgres` (PostgreSQL 16) | **Amazon RDS for PostgreSQL** |
| `redpanda` (Kafka-compatible, port 9092) | **Amazon MSK** |
| `workhub-app` (8080) | **Kubernetes Deployment on EKS** — container image from **ECR** (manifests/Helm live under `/k8s` or `/helm`, not in this folder) |
| `prometheus` (9090), `grafana` (3000) | **In-cluster** via Helm (e.g. `kube-prometheus-stack`); **not** created as Terraform resources here — see the comment block at the top of `main.tf` |

Ports aligned with compose: app **8080**, PostgreSQL **5432**, Kafka **9092**, Prometheus **9090**, Grafana **3000** (the latter two once you install charts).

## Prerequisites

- **Terraform** >= 1.5.0
- **AWS CLI** configured with credentials and a profile/role that can create: VPC, EC2 (incl. EKS nodes), EKS, RDS, MSK, ECR, IAM roles/policies, ELB-related service roles used by EKS, etc.
- **kubectl** (after cluster creation)
- Familiarity with **EKS access** (IAM principal must be mapped into `aws-auth` ConfigMap or use EKS Access Entries API — you may need a one-time admin step after first apply)

Broad IAM capability areas: **EKS**, **EC2**, **VPC**, **RDS**, **MSK**, **ECR**, **IAM**.

## Usage

```bash
cd terraform
cp terraform.tfvars.example terraform.tfvars
# Edit terraform.tfvars — set db_username, db_password (use AWS Secrets Manager in production)
terraform init
terraform plan
terraform apply
# Configure kubectl (see terraform output kubeconfig_command):
aws eks update-kubeconfig --region us-east-1 --name workhub-eks
```

Replace region and cluster name if you overrode defaults in `terraform.tfvars`.

## Testing

Run checks in this order. Steps 1–2 need **no AWS access**; step 3+ need a configured AWS account (**`aws sts get-caller-identity`** must succeed).

### 1. Format (optional but recommended)

```bash
cd terraform
terraform fmt -recursive
```

### 2. Initialize and validate (local / CI)

```bash
terraform init -upgrade
terraform validate
```

Expected: `Success! The configuration is valid.`

This run used **Terraform 1.15.x** and **hashicorp/aws ~> 5.0** (e.g. v5.100.0). Commit **`terraform/.terraform.lock.hcl`** if you want reproducible provider versions (recommended for teams).

### 3. Plan (requires AWS credentials)

Configure the CLI (`aws configure`, SSO, or env vars), then:

```bash
cp terraform.tfvars.example terraform.tfvars
# Edit terraform.tfvars — real db_username / db_password

terraform plan -out=tfplan
```

Or without a tfvars file (only for quick checks):

```bash
terraform plan -var="db_username=..." -var="db_password=..." -out=tfplan
```

If you see **No valid credential sources found**, configure AWS credentials before continuing.

### 4. Apply, then verify (creates billable resources)

```bash
terraform apply tfplan
# or: terraform apply
```

After apply:

```bash
terraform output
aws eks update-kubeconfig --region <region> --name <cluster_name>
kubectl get nodes
```

Optional: from a throwaway pod in the cluster, test RDS (`psql` to `rds_endpoint`) and confirm `msk_bootstrap_brokers` is populated once MSK is **ACTIVE**.

### 5. Destroy (teardown test)

```bash
terraform destroy
```

### Windows quick install (optional)

If Terraform or AWS CLI are missing, one approach is **winget**:

```powershell
winget install --id Hashicorp.Terraform --exact --accept-package-agreements --accept-source-agreements
winget install --id Amazon.AWSCLI --exact --accept-package-agreements --accept-source-agreements
```

Restart the terminal (or refresh `PATH`) so `terraform` and `aws` resolve.

## Mapping: docker-compose → AWS resources

| docker-compose service | AWS resource | Port |
|------------------------|--------------|------|
| app (8080) | EKS Deployment + Service / Ingress (ALB or similar) | 8080 |
| postgres (host 5433 → container 5432) | RDS PostgreSQL | 5432 |
| redpanda (9092) | MSK Kafka | 9092 |
| prometheus (9090) | In-cluster (Helm) | 9090 |
| grafana (3000) | In-cluster (Helm) | 3000 |

## Spring Boot integration

After `terraform apply`, wire the app using outputs and your Kubernetes Secrets:

| Setting | Notes |
|---------|--------|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://<rds_endpoint>:<rds_port>/<db_name>` — `<db_name>` must match `var.db_name` (default `workhubdb`). |
| `DB_HOST` | Set to `rds_endpoint` if you keep using `application.properties` `${DB_HOST}` placeholders. |
| `SPRING_DATASOURCE_USERNAME` / `DB_USERNAME` | Same as `db_username` in tfvars. |
| `SPRING_DATASOURCE_PASSWORD` / `DB_PASSWORD` | Same as `db_password`; **inject via Secret**, not plain ConfigMap in prod. |
| `KAFKA_BOOTSTRAP_SERVERS` | Use `msk_bootstrap_brokers` output (comma-separated broker list). |
| `AUTH_SECRET_KEY` / `auth.secretKey` | **Must** come from a Kubernetes Secret (or external secrets operator). Do **not** commit or hardcode — `application.properties` currently contains a placeholder dev key that must not ship to EKS as-is. |

**Note:** Repo `application.properties` defaults JDBC database name to `workhub` while Terraform defaults `db_name` to `workhubdb`. Either set `db_name = "workhub"` in `terraform.tfvars` or change the JDBC path to match `workhubdb`.

### Stack confirmation (repo)

- **Spring Boot** 4.0.x (parent POM), **Java** 21, **spring-kafka**, **JPA**, **Actuator**, **Micrometer Prometheus** — matches the app this module supports.

## Resources provisioned

| Resource | Type | Description |
|----------|------|-------------|
| VPC | `aws_vpc` | Dedicated `/16` with DNS hostnames/support. |
| Subnets ×4 | `aws_subnet` | Two public + two private across two AZs. |
| Internet Gateway | `aws_internet_gateway` | Public egress. |
| Elastic IP + NAT | `aws_eip`, `aws_nat_gateway` | Single NAT in first public subnet; private subnets route `0.0.0.0/0` to NAT. |
| Route tables | `aws_route_table`, `aws_route_table_association` | Public → IGW; private → NAT. |
| EKS cluster IAM | `aws_iam_role`, `aws_iam_role_policy_attachment` | Cluster + worker node roles with standard AWS managed policies. |
| EKS cluster | `aws_eks_cluster` | Control plane in private subnets; public + private API access enabled. |
| Launch template | `aws_launch_template` | Attaches node SG + EKS cluster SG to workers (for RDS/MSK ingress sourcing). |
| Node group | `aws_eks_node_group` | Managed nodes in private subnets; autoscaling bounds from variables. |
| RDS subnet group | `aws_db_subnet_group` | Private subnets. |
| RDS SG | `aws_security_group` | `5432` from worker node SG only. |
| RDS instance | `aws_db_instance` | PostgreSQL; encrypted storage; `skip_final_snapshot` only when `environment == "dev"`. |
| MSK SG | `aws_security_group` | `9092` from nodes; full self-ingress for broker mesh. |
| MSK cluster | `aws_msk_cluster` | Two brokers; `TLS_PLAINTEXT` client broker setting for dev parity. |
| ECR repository | `aws_ecr_repository` | `workhub-app` image home; mutable tags (dev); scan on push. |

## Variables reference

| Variable | Type | Default | Description |
|----------|------|---------|-------------|
| `project_name` | string | `workhub` | Prefix for names and tags. |
| `environment` | string | `dev` | Environment label; drives RDS final snapshot behavior. |
| `aws_region` | string | `us-east-1` | AWS region. |
| `vpc_cidr` | string | `10.0.0.0/16` | VPC CIDR. |
| `cluster_name` | string | `workhub-eks` | EKS cluster name. |
| `kubernetes_version` | string | `1.30` | EKS Kubernetes version. |
| `node_instance_type` | string | `t3.medium` | Node group instance type. |
| `desired_capacity` | number | `2` | Desired node count. |
| `min_capacity` | number | `1` | Minimum nodes. |
| `max_capacity` | number | `4` | Maximum nodes. |
| `postgres_version` | string | `16` | RDS engine version. |
| `db_instance_class` | string | `db.t3.micro` | RDS instance class. |
| `db_name` | string | `workhubdb` | Initial database name. |
| `db_username` | string | *(required)* | RDS master user. |
| `db_password` | string (sensitive) | *(required)* | RDS master password. |
| `kafka_version` | string | `3.5.1` | MSK Kafka version. |
| `msk_cluster_name` | string | `workhub-msk` | MSK cluster name. |
| `kafka_instance_type` | string | `kafka.t3.small` | MSK broker size. |
| `kafka_storage_gb` | number | `20` | Broker EBS volume size (GiB). |

## Outputs reference

| Output | Description |
|--------|-------------|
| `eks_cluster_name` | EKS cluster name. |
| `eks_cluster_endpoint` | Kubernetes API endpoint. |
| `eks_cluster_arn` | Cluster ARN. |
| `eks_node_group_arn` | Managed node group ARN. |
| `vpc_id` | WorkHub VPC ID. |
| `private_subnet_ids` | Private subnet IDs. |
| `rds_endpoint` | DB hostname for JDBC / `DB_HOST`. |
| `rds_port` | DB port (usually 5432). |
| `msk_bootstrap_brokers` | Bootstrap broker list for `KAFKA_BOOTSTRAP_SERVERS`. |
| `ecr_repository_url` | Push/pull URL for the app image. |
| `kubeconfig_command` | Ready-to-run `aws eks update-kubeconfig` command. |

## `.gitignore` additions

Add (or merge) the following so state and secrets never land in git:

```
terraform/.terraform/
terraform/terraform.tfvars
terraform/*.tfstate
terraform/*.tfstate.*
terraform/crash.log
```

**Optional:** Many teams **commit** `terraform/.terraform.lock.hcl` for provider reproducibility; others ignore it. Pick one policy for your org.

## Destroy

```bash
terraform destroy
```

**WARNING:** This destroys the RDS instance and MSK cluster. For `environment != "dev"`, a final DB snapshot name is set on destroy — verify backups and retention policies before running destroy in shared environments.
