/*
# ==============================================================================
# 1. Infra EC2용 보안 그룹
# 앱 서버(ASG)에서만 Redis(6379), Kafka(9092), Prometheus(9090), Grafana(3000) 접근 허용
# ==============================================================================
resource "aws_security_group" "infra_sg" {
  name   = "moa-v2-infra-sg"
  vpc_id = aws_vpc.main_vpc.id

  # Redis
  ingress {
    from_port   = 6379
    to_port     = 6379
    protocol    = "tcp"
    cidr_blocks = ["10.0.0.0/16"]
  }

  # Kafka
  ingress {
    from_port   = 9092
    to_port     = 9092
    protocol    = "tcp"
    cidr_blocks = ["10.0.0.0/16"]
  }

  # Prometheus
  ingress {
    from_port   = 9090
    to_port     = 9090
    protocol    = "tcp"
    cidr_blocks = ["10.0.0.0/16"]
  }

  # Grafana
  ingress {
    from_port   = 3000
    to_port     = 3000
    protocol    = "tcp"
    cidr_blocks = ["10.0.0.0/16"]
  }

  # SSM 접속용 아웃바운드 전체 허용
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = { Name = "moa-v2-infra-sg" }
}
*/

# ==============================================================================
# 2. SSM을 위한 IAM Role
# 키페어 없이 AWS SSM으로 EC2에 접속하기 위한 권한 설정
# ==============================================================================
resource "aws_iam_role" "infra_ssm_role" {
  name = "moa-v2-infra-ssm-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action    = "sts:AssumeRole"
      Effect    = "Allow"
      Principal = { Service = "ec2.amazonaws.com" }
    }]
  })
}

resource "aws_iam_role_policy_attachment" "infra_ssm_policy" {
  role       = aws_iam_role.infra_ssm_role.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

# 추가: NAT 인스턴스가 앱 서버의 동적 IP를 조회하기 위해 필요한 IAM 정책 (AWS CLI 사용)
resource "aws_iam_role_policy" "infra_describe_ec2" {
  name = "moa-v2-infra-describe-ec2"
  role = aws_iam_role.infra_ssm_role.name

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action   = [
        "ec2:DescribeInstances",
        "autoscaling:DescribeAutoScalingGroups"
      ]
      Effect   = "Allow"
      Resource = "*"
    }]
  })
}

resource "aws_iam_role_policy" "infra_route53_backend_dns" {
  name = "moa-v2-infra-route53-backend-dns"
  role = aws_iam_role.infra_ssm_role.name

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action = [
        "route53:ChangeResourceRecordSets",
        "route53:ListResourceRecordSets"
      ]
      Effect   = "Allow"
      Resource = aws_route53_zone.private_backend_zone.arn
    }]
  })
}

resource "aws_iam_role_policy" "infra_s3_upload" {
  name = "moa-v2-infra-s3-upload"
  role = aws_iam_role.infra_ssm_role.name

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action   = [
        "s3:PutObject",
        "s3:GetObject",
        "s3:DeleteObject"
      ]
      Effect   = "Allow"
      Resource = "${aws_s3_bucket.moa_cdn_bucket.arn}/*"
    }]
  })
}

resource "aws_iam_instance_profile" "infra_profile" {
  name = "moa-v2-infra-profile"
  role = aws_iam_role.infra_ssm_role.name
}

/*
# ==============================================================================
# 3. Infra EC2 생성
# Redis + Kafka + Prometheus + Grafana를 Docker로 실행
# Swap 4GB 설정으로 1GB RAM 한계 극복
# ==============================================================================
# 로컬 인프라 EC2는 PaaS(Aiven 등) 사용을 위해 주석 처리됨
resource "aws_instance" "infra_instance" {
  ami                    = data.aws_ami.amazon_linux.id
  instance_type          = "t2.micro"
  subnet_id              = aws_subnet.private_a.id
  vpc_security_group_ids = [aws_security_group.infra_sg.id]
  iam_instance_profile   = aws_iam_instance_profile.infra_profile.name

  # EBS 30GB (프리티어 한도)
  root_block_device {
    volume_size = 25
    volume_type = "gp2"
  }

  user_data = <<-EOF
    #!/bin/bash

    # 1. Swap 4GB 설정 (1GB RAM 한계 극복)
    fallocate -l 4G /swapfile
    chmod 600 /swapfile
    mkswap /swapfile
    swapon /swapfile
    echo '/swapfile swap swap defaults 0 0' >> /etc/fstab

    # 2. Docker 설치
    dnf install -y docker
    systemctl enable docker
    systemctl start docker

    # 3. Docker Compose 설치
    curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
    chmod +x /usr/local/bin/docker-compose

    # 4. docker-compose.yml 작성
    mkdir -p /opt/infra
    cat > /opt/infra/docker-compose.yml <<COMPOSE
    version: '3.8'
    services:
      redis:
        image: redis:7-alpine
        container_name: redis
        ports:
          - "6379:6379"
        restart: always

      kafka:
        image: apache/kafka:latest
        container_name: kafka
        ports:
          - "9092:9092"
        environment:
          KAFKA_NODE_ID: 1
          KAFKA_PROCESS_ROLES: broker,controller
          KAFKA_LISTENERS: PLAINTEXT://0.0.0.0:9092,CONTROLLER://0.0.0.0:9093
          KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://$(hostname -I | awk '{print $1}'):9092
          KAFKA_CONTROLLER_LISTENER_NAMES: CONTROLLER
          KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,CONTROLLER:PLAINTEXT
          KAFKA_CONTROLLER_QUORUM_VOTERS: 1@localhost:9093
          KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
          KAFKA_HEAP_OPTS: "-Xms512m -Xmx512m"
        restart: always

      prometheus:
        image: prom/prometheus:latest
        container_name: prometheus
        ports:
          - "9090:9090"
        restart: always

      grafana:
        image: grafana/grafana:latest
        container_name: grafana
        ports:
          - "3000:3000"
        environment:
          GF_SECURITY_ADMIN_PASSWORD: admin
        restart: always
    COMPOSE

    # 5. Docker Compose 실행
    cd /opt/infra
    docker-compose up -d
  EOF

  tags = { Name = "moa-v2-infra-instance" }
}
*/