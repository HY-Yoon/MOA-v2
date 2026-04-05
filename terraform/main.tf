terraform {
  backend "s3" {
    bucket         = "moa-v2-tfstate"
    key            = "terraform.tfstate"
    region         = "ap-northeast-2"
    dynamodb_table = "moa-v2-terraform-lock"
    encrypt        = true
  }
}
# ==============================================================================
# 1. AWS Provider 설정
# 테라폼이 AWS 클라우드의 어느 지역(Region)에 인프라를 만들지 지정합니다.
# 포트폴리오 설계서에 명시된 대로 한국(서울) 리전인 ap-northeast-2를 사용합니다[cite: 25].
# ==============================================================================
provider "aws" {
  region = "ap-northeast-2" 
}

# ==============================================================================
# 2. VPC (Virtual Private Cloud) 생성
# AWS 내에 우리 프로젝트만의 격리된 가상 네트워크 울타리를 만듭니다.
# CIDR을 10.0.0.0/16으로 설정하여 넉넉한 IP 대역을 확보합니다[cite: 25].
# ==============================================================================
resource "aws_vpc" "main_vpc" {
  cidr_block           = "10.0.0.0/16"
  enable_dns_hostnames = true  # EC2 인스턴스가 퍼블릭 DNS 이름을 갖도록 허용 (필수)
  enable_dns_support   = true

  tags = {
    Name = "moa-v2-vpc"
  }
}

# ==============================================================================
# 3. Internet Gateway (IGW) 생성
# VPC 내부의 자원들이 바깥 인터넷 세상과 통신할 수 있게 해주는 '대문' 역할을 합니다.
# ==============================================================================
resource "aws_internet_gateway" "igw" {
  vpc_id = aws_vpc.main_vpc.id

  tags = {
    Name = "moa-v2-igw"
  }
}

# ==============================================================================
# 4. Public Subnet (퍼블릭 서브넷) 2개 생성
# 외부 인터넷과 직접 연결되는 구역입니다. 주로 로드밸런서(ALB)나 NAT 인스턴스가 위치합니다.
# 고가용성을 위해 a구역과 c구역 두 곳에 나누어 만듭니다[cite: 25].
# ==============================================================================
resource "aws_subnet" "public_a" {
  vpc_id                  = aws_vpc.main_vpc.id
  cidr_block              = "10.0.1.0/24"
  availability_zone       = "ap-northeast-2a"
  map_public_ip_on_launch = true # 여기에 생성되는 EC2는 자동으로 퍼블릭 IP를 받습니다.

  tags = { Name = "moa-v2-public-a" }
}

resource "aws_subnet" "public_c" {
  vpc_id                  = aws_vpc.main_vpc.id
  cidr_block              = "10.0.2.0/24"
  availability_zone       = "ap-northeast-2c"
  map_public_ip_on_launch = true

  tags = { Name = "moa-v2-public-c" }
}

# ==============================================================================
# 5. Private Subnet (프라이빗 서브넷) 2개 생성
# 외부 인터넷에서 직접 접근할 수 없는 안전하고 깊숙한 구역입니다. 
# 실제 백엔드 앱 서버(Spring Boot)와 데이터베이스(RDS)가 여기에 위치하여 보안을 챙깁니다[cite: 23].
# ==============================================================================
resource "aws_subnet" "private_a" {
  vpc_id            = aws_vpc.main_vpc.id
  cidr_block        = "10.0.3.0/24"
  availability_zone = "ap-northeast-2a"

  tags = { Name = "moa-v2-private-a" }
}

resource "aws_subnet" "private_c" {
  vpc_id            = aws_vpc.main_vpc.id
  cidr_block        = "10.0.4.0/24"
  availability_zone = "ap-northeast-2c"

  tags = { Name = "moa-v2-private-c" }
}

# ==============================================================================
# 6. Public Route Table (퍼블릭 라우팅 테이블)
# 퍼블릭 서브넷의 네트워크 트래픽 이정표입니다. "인터넷으로 가는 길(0.0.0.0/0)은 IGW로 가라"고 지시합니다.
# ==============================================================================
resource "aws_route_table" "public_rt" {
  vpc_id = aws_vpc.main_vpc.id
  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.igw.id
  }
  tags = { Name = "moa-v2-public-rt" }
}

# 퍼블릭 라우팅 테이블을 퍼블릭 서브넷 2개와 연결
resource "aws_route_table_association" "public_a_assoc" {
  subnet_id      = aws_subnet.public_a.id
  route_table_id = aws_route_table.public_rt.id
}
resource "aws_route_table_association" "public_c_assoc" {
  subnet_id      = aws_subnet.public_c.id
  route_table_id = aws_route_table.public_rt.id
}

# ==============================================================================
# 7.NAT + Nginx 통합 Instance 구성
# ==============================================================================
resource "aws_security_group" "nat_sg" {
  name        = "moa-v2-nat-sg"
  vpc_id      = aws_vpc.main_vpc.id

  # 프라이빗 서브넷에서 오는 요청 허용 (NAT 용도)
  ingress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["10.0.3.0/24", "10.0.4.0/24"]
  }

  # 외부 접속 허용: HTTP 80 (Nginx 프록시 용도)
  ingress {
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # 외부 접속 허용: HTTPS 443 (Nginx 프록시 용도)
  ingress {
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
  tags = { Name = "moa-v2-nat-sg" }
}

data "aws_ami" "amazon_linux" {
  most_recent = true
  owners      = ["amazon"]
  filter {
    name   = "name"
    values = ["al2023-ami-2023.*-x86_64"]
  }
}

resource "aws_instance" "nat_instance" {
  ami                    = data.aws_ami.amazon_linux.id
  instance_type          = "t2.micro"
  subnet_id              = aws_subnet.public_a.id
  vpc_security_group_ids = [aws_security_group.nat_sg.id]
  iam_instance_profile   = aws_iam_instance_profile.infra_profile.name

  source_dest_check = false
  user_data_replace_on_change = true

  user_data = templatefile("${path.module}/scripts/nat-init.sh.tftpl", {
    aws_region          = var.nat_proxy_region
    app_asg_name        = var.nat_proxy_app_asg_name
    app_tag_name        = var.nat_proxy_app_name_tag
    public_domain       = var.nat_proxy_public_domain
    backend_app_port    = var.nat_proxy_backend_port
    backend_dns_zone_id = aws_route53_zone.private_backend_zone.zone_id
    backend_dns_name    = trimsuffix(aws_route53_record.private_backend_record.fqdn, ".")
  })

  tags = { Name = "moa-v2-nat-instance" }
}

# ==============================================================================
# 7-1. 탄력적 IP (Elastic IP) 생성 및 연결
# NAT 인스턴스가 고정된 퍼블릭 IP를 갖도록 설정합니다. (도메인 연결용)
# ==============================================================================
resource "aws_eip" "nat_eip" {
  instance = aws_instance.nat_instance.id
  domain   = "vpc"

  tags = { Name = "moa-v2-nat-eip" }
}

# ==============================================================================
# 7. Private Route Table (프라이빗 라우팅 테이블)
# 프라이빗 서브넷의 이정표입니다. "인터넷으로 나갈 일(0.0.0.0/0)이 생기면 방금 만든 NAT EC2로 가라"고 지시합니다.
# ==============================================================================
resource "aws_route_table" "private_rt" {
  vpc_id = aws_vpc.main_vpc.id
  route {
    cidr_block           = "0.0.0.0/0"
    network_interface_id = aws_instance.nat_instance.primary_network_interface_id 
  }
  tags = { Name = "moa-v2-private-rt" }
}

# 프라이빗 라우팅 테이블을 프라이빗 서브넷 2개와 연결
resource "aws_route_table_association" "private_a_assoc" {
  subnet_id      = aws_subnet.private_a.id
  route_table_id = aws_route_table.private_rt.id
}
resource "aws_route_table_association" "private_c_assoc" {
  subnet_id      = aws_subnet.private_c.id
  route_table_id = aws_route_table.private_rt.id
}

output "public_subnet_a_id" {
  value = aws_subnet.public_a.id
}

output "nat_public_ip" {
  value       = aws_eip.nat_eip.public_ip
  description = "NAT Instance Public IP for Domain A Record"
}

# ==============================================================================
# 8. Route 53 (DNS 자동 연결)
# NAT 인스턴스의 탄력적 IP(EIP)를 moa.hee-factory.com 도메인에 자동으로 매핑합니다.
# ==============================================================================

# AWS Route 53에 이미 존재하는 hee-factory.com 도메인의 정보를 찾아옵니다.
data "aws_route53_zone" "main_domain" {
  name         = "hee-factory.com"
  private_zone = false
}

# moa.hee-factory.com의 A 레코드를 생성하고 EIP 주소를 자동으로 꽂아넣습니다.
resource "aws_route53_record" "moa_a_record" {
  zone_id = data.aws_route53_zone.main_domain.zone_id
  name    = "moa.${data.aws_route53_zone.main_domain.name}" # moa.hee-factory.com
  type    = "A"
  ttl     = 300

  # 값(Value): Terraform이 방금 발급받은 NAT 인스턴스의 EIP
  records = [aws_eip.nat_eip.public_ip]
}

# 앱 서버 동적 탐색용 Private Hosted Zone
resource "aws_route53_zone" "private_backend_zone" {
  name = var.nat_proxy_private_zone_name

  vpc {
    vpc_id = aws_vpc.main_vpc.id
  }

  tags = { Name = "moa-v2-private-backend-zone" }
}

# NAT nginx가 참조할 백엔드 DNS 레코드 (실제 값은 NAT 스크립트가 주기적으로 UPSERT)
resource "aws_route53_record" "private_backend_record" {
  zone_id = aws_route53_zone.private_backend_zone.zone_id
  name    = var.nat_proxy_backend_dns_name
  type    = "A"
  ttl     = 10
  records = ["127.0.0.1"]
}