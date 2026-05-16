output "eks_cluster_name" {
  description = "Name of the EKS cluster; use with aws eks update-kubeconfig."
  value       = aws_eks_cluster.main.name
}

output "eks_cluster_endpoint" {
  description = "Kubernetes API server endpoint for the EKS cluster."
  value       = aws_eks_cluster.main.endpoint
}

output "eks_cluster_arn" {
  description = "Amazon Resource Name of the EKS cluster."
  value       = aws_eks_cluster.main.arn
}

output "eks_node_group_arn" {
  description = "ARN of the managed EKS node group."
  value       = aws_eks_node_group.main.arn
}

output "vpc_id" {
  description = "ID of the WorkHub VPC."
  value       = aws_vpc.main.id
}

output "private_subnet_ids" {
  description = "Private subnet IDs (worker nodes, RDS, MSK)."
  value       = aws_subnet.private[*].id
}

output "rds_endpoint" {
  description = "RDS hostname; use as the host in SPRING_DATASOURCE_URL (or DB_HOST)."
  value       = aws_db_instance.main.address
}

output "rds_port" {
  description = "RDS PostgreSQL port (typically 5432)."
  value       = aws_db_instance.main.port
}

output "msk_bootstrap_brokers" {
  description = "Comma-separated PLAINTEXT bootstrap brokers for KAFKA_BOOTSTRAP_SERVERS (matches TLS_PLAINTEXT client_broker setting)."
  value       = aws_msk_cluster.main.bootstrap_brokers
}

output "ecr_repository_url" {
  description = "Registry URL for the WorkHub application image."
  value       = aws_ecr_repository.app.repository_url
}

output "kubeconfig_command" {
  description = "Shell command to merge EKS credentials into ~/.kube/config for kubectl."
  value       = "aws eks update-kubeconfig --region ${var.aws_region} --name ${aws_eks_cluster.main.name}"
}
