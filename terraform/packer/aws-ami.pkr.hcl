packer {
  required_plugins {
    amazon = {
      version = ">= 1.3.0"
      source  = "github.com/hashicorp/amazon"
    }
  }
}

source "amazon-ebs" "moa_backend" {
  access_key = env("AWS_ACCESS_KEY_ID")
  secret_key = env("AWS_SECRET_ACCESS_KEY")
  region     = var.aws_region

  instance_type = var.instance_type
  ssh_username  = "ec2-user"
  subnet_id     = var.subnet_id

  ami_name        = "${var.app_name}-${formatdate("YYYYMMDDhhmmss", timestamp())}"
  ami_description = "AMI built by Packer for ${var.app_name}"

  source_ami_filter {
    filters = {
      name                = "al2023-ami-2023.*-x86_64"
      root-device-type    = "ebs"
      virtualization-type = "hvm"
    }
    most_recent = true
    owners      = ["amazon"]
  }

  tags = {
    Name      = var.app_name
    ManagedBy = "packer"
    Service   = "backend"
  }
}

build {
  name    = "moa-backend-ami"
  sources = ["source.amazon-ebs.moa_backend"]

  provisioner "file" {
    source      = var.app_artifact_path
    destination = "/tmp/app.jar"
  }

  provisioner "file" {
    source      = "./files/moa-backend.service"
    destination = "/tmp/moa-backend.service"
  }

  provisioner "shell" {
    script = "./scripts/setup.sh"
  }

  provisioner "shell" {
    script = "./scripts/configure-service.sh"
  }
}
