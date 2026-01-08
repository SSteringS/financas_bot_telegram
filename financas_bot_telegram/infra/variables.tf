variable "env" {
  description = "Ambiente (dev ou prod"
  type        = string
}

variable "project_id" {
  description = "ID unico do projeto"
  type        = string
  default     = "finbot"
}

variable "db_instance_class" {
  description = "Tipo da instancia do RDS"
  type        = string
}

variable "db_username" {
  description = "Username mestre do RDS"
  type        = string
  default     = "fin_master_user" # Altere aqui para o que preferir
}