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

  user_data = templatefile("${path.module}/provision/bootstrap.sh", {
    domain_name = var.domain_name
  })

  tags = { Name = "finbot-${var.env}-ec2-app" }

  # AMI atualizada pelo AWS após o provisionamento inicial — ignorar drift pra não recriar instância de prod.
  lifecycle {
    ignore_changes = [ami]
  }

  root_block_device {
    volume_size = 10
    volume_type = "gp3"
  }
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
