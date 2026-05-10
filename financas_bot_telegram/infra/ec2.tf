# AMI mais recente do Amazon Linux 2023 ARM64 (compatível com t4g)
data "aws_ami" "amazon_linux_2023" {
  most_recent = true
  owners      = ["amazon"]

  filter {
    name   = "name"
    values = ["al2023-ami-*-arm64"]
  }

  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }
}

# EC2 — aplicação Spring Boot
resource "aws_instance" "finbot_app" {
  ami                    = data.aws_ami.amazon_linux_2023.id
  instance_type          = var.ec2_instance_type
  subnet_id              = aws_subnet.public.id
  vpc_security_group_ids = [aws_security_group.ec2_sg.id]
  key_name               = var.key_pair_name
  iam_instance_profile   = aws_iam_instance_profile.ec2_profile.name

  user_data = <<-EOF
    #!/bin/bash
    dnf update -y
    dnf install -y java-21-amazon-corretto-headless
    mkdir -p /opt/finbot
    useradd -r -s /bin/false finbot
    chown finbot:finbot /opt/finbot
  EOF

  tags = { Name = "finbot-${var.env}-ec2-app" }
}

# Elastic IP — endereço fixo para configurar o webhook do Telegram
resource "aws_eip" "finbot_eip" {
  instance = aws_instance.finbot_app.id
  domain   = "vpc"

  tags = { Name = "finbot-${var.env}-eip" }
}

output "ec2_public_ip" {
  description = "IP público fixo da EC2 — use para configurar o webhook do Telegram"
  value       = aws_eip.finbot_eip.public_ip
}
