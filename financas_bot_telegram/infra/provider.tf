terraform {
  required_version = ">= 1.5.0"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = "us-east-1"

  default_tags {
    tags = {
      Project   = "finbot"
      Env       = var.env # Usando a variável que definimos!
      ManagedBy = "terraform"
      Owner     = "Satyan"
    }
  }
}