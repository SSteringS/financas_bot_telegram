# Lambda removido — aplicação migrada para EC2 t4g.micro
# Motivo: Lambda em subnet privada exigiria NAT Gateway ($32/mês)
#         e Spring Boot tem cold starts pesados mesmo com SnapStart.
#         Para 2 usuários, EC2 é mais simples, mais barato e mais fácil de debugar.
