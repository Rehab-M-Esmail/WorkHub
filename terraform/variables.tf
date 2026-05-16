variable "project_name" {
  description = "Short project prefix for resource names and tags (e.g. workhub)."
  type        = string
  default     = "workhub"
}

variable "environment" {
  description = "Deployment environment (e.g. dev, staging, prod). Drives snapshot behavior for RDS."
  type        = string
  default     = "dev"
}

variable "aws_region" {
  description = "AWS region for all resources."
  type        = string
  default     = "us-east-1"
}

variable "vpc_cidr" {
  description = "IPv4 CIDR for the dedicated WorkHub VPC."
  type        = string
  default     = "10.0.0.0/16"
}

variable "cluster_name" {
  description = "EKS cluster name."
  type        = string
  default     = "workhub-eks"
}

variable "kubernetes_version" {
  description = "EKS control plane Kubernetes version."
  type        = string
  default     = "1.30"
}

variable "node_instance_type" {
  description = "EC2 instance type for the managed node group."
  type        = string
  default     = "t3.medium"
}

variable "desired_capacity" {
  description = "Desired worker node count for the EKS node group."
  type        = number
  default     = 2
}

variable "min_capacity" {
  description = "Minimum worker nodes (autoscaling floor)."
  type        = number
  default     = 1
}

variable "max_capacity" {
  description = "Maximum worker nodes (autoscaling ceiling)."
  type        = number
  default     = 4
}

variable "postgres_version" {
  description = "RDS PostgreSQL major.minor version family."
  type        = string
  default     = "16"
}

variable "db_instance_class" {
  description = "RDS instance class."
  type        = string
  default     = "db.t3.micro"
}

variable "db_name" {
  description = "Initial database name created on the RDS instance."
  type        = string
  default     = "workhubdb"
}

variable "db_username" {
  description = "Master username for RDS PostgreSQL (must be supplied via tfvars or env)."
  type        = string
}

variable "db_password" {
  description = "Master password for RDS PostgreSQL (must be supplied via tfvars or env; use Secrets Manager in prod)."
  type        = string
  sensitive   = true
}

variable "kafka_version" {
  description = "Apache Kafka version for the MSK cluster."
  type        = string
  default     = "3.5.1"
}

variable "msk_cluster_name" {
  description = "Logical name of the MSK cluster."
  type        = string
  default     = "workhub-msk"
}

variable "kafka_instance_type" {
  description = "MSK broker EC2 instance type."
  type        = string
  default     = "kafka.t3.small"
}

variable "kafka_storage_gb" {
  description = "EBS volume size (GiB) per MSK broker."
  type        = number
  default     = 20
}
