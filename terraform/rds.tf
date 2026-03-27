variable "db_name" {
  description = "RDS database name"
  type        = string
  default     = "defaultdb"
}

variable "db_username" {
  description = "RDS master username"
  type        = string
  default     = "avnadmin"
}

variable "db_password" {
  description = "RDS master password"
  type        = string
  sensitive   = true
}

resource "aws_db_subnet_group" "moa_db_subnet_group" {
  name       = "moa-v2-db-subnet-group"
  subnet_ids = [aws_subnet.private_a.id, aws_subnet.private_c.id]

  tags = {
    Name = "moa-v2-db-subnet-group"
  }
}

resource "aws_security_group" "rds_sg" {
  name   = "moa-v2-rds-sg"
  vpc_id = aws_vpc.main_vpc.id

  # Temporary rule: allow PostgreSQL from private subnet CIDRs.
  # Replace with security_groups = [aws_security_group.app_sg.id] after app SG is finalized.
  ingress {
    from_port   = 5432
    to_port     = 5432
    protocol    = "tcp"
    cidr_blocks = ["10.0.3.0/24", "10.0.4.0/24"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "moa-v2-rds-sg"
  }
}

resource "aws_db_instance" "moa_postgres" {
  identifier = "moa-v2-postgres"

  engine         = "postgres"
  engine_version = "16.3"
  instance_class = "db.t3.micro"

  allocated_storage     = 20
  max_allocated_storage = 100
  storage_type          = "gp3"
  storage_encrypted     = true

  db_name  = var.db_name
  username = var.db_username
  password = var.db_password
  port     = 5432

  db_subnet_group_name   = aws_db_subnet_group.moa_db_subnet_group.name
  vpc_security_group_ids = [aws_security_group.rds_sg.id]

  publicly_accessible     = false
  multi_az                = false
  backup_retention_period = 7
  deletion_protection     = false
  skip_final_snapshot     = true
  apply_immediately       = true

  tags = {
    Name = "moa-v2-postgres"
  }
}

output "rds_endpoint" {
  value = aws_db_instance.moa_postgres.address
}

output "rds_port" {
  value = aws_db_instance.moa_postgres.port
}
