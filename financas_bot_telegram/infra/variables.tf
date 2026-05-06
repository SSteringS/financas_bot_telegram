variable "env" {
  description = "Ambiente (dev ou prod)"
  type        = string
}

variable "ec2_instance_type" {
  description = "Tipo da instância EC2"
  type        = string
  default     = "t4g.micro"
}

variable "key_pair_name" {
  description = "Nome do Key Pair EC2 para acesso SSH (criado manualmente no console AWS)"
  type        = string
}

variable "my_ip" {
  description = "Seu IP público com máscara para acesso SSH (ex: 187.45.123.67/32)"
  type        = string
}

variable "s3_images_bucket" {
  description = "Nome do bucket S3 para armazenar imagens dos pagamentos"
  type        = string
  default     = "bot-financas-pagamentos-satyan"
}
