variable "aws_region" {
  type        = string
  description = "AWS region for AMI build"
  default     = "ap-northeast-2"
}

variable "subnet_id" {
  type        = string
  description = "Subnet ID used by temporary Packer EC2 instance"
}

variable "app_name" {
  type        = string
  description = "Application name used for AMI naming and tags"
  default     = "moa-backend"
}

variable "instance_type" {
  type        = string
  description = "EC2 instance type for AMI build"
  default     = "t2.micro"
}

variable "app_artifact_path" {
  type        = string
  description = "Local path to backend JAR uploaded during Packer build"
  default     = "../../@backend/build/libs/app.jar"
}
