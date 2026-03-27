# ==============================================================================
# 1. 최신 백엔드 AMI 자동 조회
# 방금 Packer가 구워낸 가장 따끈따끈한 AMI를 자동으로 찾아옵니다.
# ==============================================================================
data "aws_ami" "latest_backend" {
  most_recent = true
  owners      = ["self"] # 내 계정에서 만든 AMI만 검색

  # packer 파일에 정의된 태그(Service=backend)를 기준으로 찾습니다.
  filter {
    name   = "tag:Service"
    values = ["backend"]
  }
}

# ==============================================================================
# 2. App 서버용 보안 그룹 (ASG)
# NAT+Nginx 서버에서 오는 요청만 받도록 설정하여 보안을 극대화합니다.
# ==============================================================================
resource "aws_security_group" "app_sg" {
  name        = "moa-v2-app-sg"
  vpc_id      = aws_vpc.main_vpc.id

  # Nginx(NAT EC2)가 8080 포트로 넘겨주는 트래픽만 허용
  ingress {
    from_port       = 8080
    to_port         = 8080
    protocol        = "tcp"
    security_groups = [aws_security_group.nat_sg.id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = { Name = "moa-v2-app-sg" }
}

# ==============================================================================
# 3. Launch Template (시작 템플릿)
# ASG가 서버를 찍어낼 때 사용할 "거푸집" 역할을 합니다.
# ==============================================================================
resource "aws_launch_template" "app_lt" {
  name_prefix   = "moa-v2-app-"
  image_id      = data.aws_ami.latest_backend.id

  # 설계서 정책에 따라 App 서버는 t3.micro를 사용합니다[cite: 15].
  instance_type = "t3.micro"

  vpc_security_group_ids = [aws_security_group.app_sg.id]

  # infra.tf에서 만들어둔 SSM 접속 권한 프로필을 재사용합니다 (키페어 없이 접속 가능)
  iam_instance_profile {
    name = aws_iam_instance_profile.infra_profile.name
  }

  # EC2가 켜질 때 실행될 스크립트 (요청하신 JVM 힙 설정 적용)
  user_data = base64encode(<<-EOF
    #!/bin/bash
    export SPRING_PROFILES_ACTIVE=prod
  EOF
  )
}

# ==============================================================================
# 4. Auto Scaling Group (ASG) - Spot Instance 구성
# ==============================================================================
resource "aws_autoscaling_group" "app_asg" {
  name                = "moa-v2-app-asg"

  # 앱 서버는 외부에서 직접 접근할 수 없는 프라이빗 서브넷에 안전하게 배치합니다[cite: 54].
  vpc_zone_identifier = [aws_subnet.private_a.id, aws_subnet.private_c.id]

  desired_capacity    = 1
  min_size            = 1
  max_size            = 2

  # 100% Spot Instance로만 띄워서 비용을 극한으로 최적화합니다[cite: 15].
  mixed_instances_policy {
    launch_template {
      launch_template_specification {
        launch_template_id = aws_launch_template.app_lt.id
        version            = "$Latest"
      }
    }
    instances_distribution {
      on_demand_base_capacity                  = 0
      on_demand_percentage_above_base_capacity = 0
      spot_allocation_strategy                 = "lowest-price"
    }
  }

  tag {
    key                 = "Name"
    value               = "moa-v2-app-instance"
    propagate_at_launch = true
  }
}