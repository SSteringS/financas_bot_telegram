# 1. Grupo de Subnets do RDS
resource "aws_db_subnet_group" "finbot_db_subnet_group" {
  name       = "finbot-${var.env}-db-subnet-group"
  # NOTA: O RDS requer subnets em pelo menos duas AZs diferentes.
  # Se o seu network.tf tiver apenas uma, este bloco falhará no apply.
  subnet_ids = [aws_subnet.private.id, aws_subnet.private_2.id]

  tags = {
    Name = "finbot-${var.env}-db-subnet-group"
  }
}

# 2. Security Group do Banco
resource "aws_security_group" "finbot_db_sg" {
  name        = "finbot-${var.env}-sg-database"
  description = "Permite acesso a base MySQL apenas para a Lambda do Bot"
  vpc_id      = aws_vpc.main.id

  ingress {
    description     = "Acesso MySQL vindo da Lambda"
    from_port       = 3306
    to_port         = 3306
    protocol        = "tcp"
    security_groups = [aws_security_group.lambda_sg.id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "finbot-${var.env}-sg-database"
  }
}

# 3. Instância RDS MySQL
resource "aws_db_instance" "finbot_mysql" {
  identifier        = "finbot-${var.env}-mysql"
  engine            = "mysql"
  engine_version    = "8.0"
  instance_class    = var.db_instance_class
  allocated_storage = 20
  storage_type      = "gp3"

  db_name  = "finbot_${var.env}"
  username = var.db_username
  password = "mudar_no_secrets"

  db_subnet_group_name   = aws_db_subnet_group.finbot_db_subnet_group.name
  vpc_security_group_ids = [aws_security_group.finbot_db_sg.id]

  skip_final_snapshot     = true
  publicly_accessible     = false
  backup_retention_period = 7

  tags = {
    Name = "finbot-${var.env}-mysql"
  }

  lifecycle {
    ignore_changes = [password]
  }
}