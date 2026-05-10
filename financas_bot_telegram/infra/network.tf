# VPC Principal
resource "aws_vpc" "main" {
  cidr_block           = "10.0.0.0/16"
  enable_dns_hostnames = true
  enable_dns_support   = true

  tags = { Name = "finbot-${var.env}-vpc-main" }
}

# Subnet Privada 1 — RDS (us-east-1a)
resource "aws_subnet" "private" {
  vpc_id            = aws_vpc.main.id
  cidr_block        = "10.0.1.0/24"
  availability_zone = "us-east-1a"

  tags = { Name = "finbot-${var.env}-subnet-private-1" }
}

# Subnet Privada 2 — RDS (us-east-1b)
resource "aws_subnet" "private_2" {
  vpc_id            = aws_vpc.main.id
  cidr_block        = "10.0.2.0/24"
  availability_zone = "us-east-1b"

  tags = { Name = "finbot-${var.env}-subnet-private-2" }
}

# Subnet Pública — EC2 (us-east-1a)
resource "aws_subnet" "public" {
  vpc_id            = aws_vpc.main.id
  cidr_block        = "10.0.3.0/24"
  availability_zone = "us-east-1a"

  tags = { Name = "finbot-${var.env}-subnet-public" }
}

# Internet Gateway — permite EC2 acessar a internet (Telegram API)
resource "aws_internet_gateway" "main" {
  vpc_id = aws_vpc.main.id

  tags = { Name = "finbot-${var.env}-igw" }
}

# Route Table pública — roteia tráfego externo via IGW
resource "aws_route_table" "public" {
  vpc_id = aws_vpc.main.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.main.id
  }

  tags = { Name = "finbot-${var.env}-rt-public" }
}

resource "aws_route_table_association" "public" {
  subnet_id      = aws_subnet.public.id
  route_table_id = aws_route_table.public.id
}

# S3 Gateway Endpoint — EC2 acessa S3 sem passar pela internet (gratuito)
resource "aws_vpc_endpoint" "s3" {
  vpc_id            = aws_vpc.main.id
  service_name      = "com.amazonaws.us-east-1.s3"
  vpc_endpoint_type = "Gateway"
  route_table_ids   = [aws_route_table.public.id]

  tags = { Name = "finbot-${var.env}-vpce-s3" }
}

# Security Group da EC2
resource "aws_security_group" "ec2_sg" {
  name        = "finbot-${var.env}-sg-ec2"
  description = "Security group da EC2"
  vpc_id      = aws_vpc.main.id

  # Webhook do Telegram — apenas a porta que o Spring Boot escuta
  ingress {
    description = "Telegram webhook"
    from_port   = 8443
    to_port     = 8443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # SSH aberto — proteção real é a chave privada; necessário para GitHub Actions
  ingress {
    description = "SSH"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # Saída livre — para Telegram API, Secrets Manager, S3
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = { Name = "finbot-${var.env}-sg-ec2" }
}

output "ec2_sg_id" {
  description = "ID do Security Group da EC2 — adicione como inbound rule no SG do RDS (porta 3306)"
  value       = aws_security_group.ec2_sg.id
}
