# VPC Principal
resource "aws_vpc" "main" {
  cidr_block           = "10.0.0.0/16"
  enable_dns_hostnames = true
  enable_dns_support   = true

  tags = { Name = "finbot-${var.env}-vpc-main" }
}

# Subnet Privada 1 (us-east-1a)
resource "aws_subnet" "private" {
  vpc_id            = aws_vpc.main.id
  cidr_block        = "10.0.1.0/24"
  availability_zone = "us-east-1a"

  tags = { Name = "finbot-${var.env}-subnet-private-1" }
}

# Subnet Privada 2 (us-east-1b) - ADICIONADA PARA O RDS FUNCIONAR
resource "aws_subnet" "private_2" {
  vpc_id            = aws_vpc.main.id
  cidr_block        = "10.0.2.0/24"
  availability_zone = "us-east-1b"

  tags = { Name = "finbot-${var.env}-subnet-private-2" }
}

# S3 Gateway Endpoint
resource "aws_vpc_endpoint" "s3" {
  vpc_id            = aws_vpc.main.id
  service_name      = "com.amazonaws.us-east-1.s3"
  vpc_endpoint_type = "Gateway"
  route_table_ids   = [aws_vpc.main.main_route_table_id]

  tags = { Name = "finbot-${var.env}-vpce-s3" }
}

# Security Group da Lambda
resource "aws_security_group" "lambda_sg" {
  name        = "finbot-${var.env}-sg-lambda"
  description = "Security group para a funcao Lambda"
  vpc_id      = aws_vpc.main.id

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = { Name = "finbot-${var.env}-sg-lambda" }
}