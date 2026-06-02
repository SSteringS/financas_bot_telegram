---
name: Feedback — sem type assertion sem validação
description: Proibido usar `as` (type assertion) sem validação correspondente no código TypeScript
type: feedback
---

Nunca usar `as TypeX` em TypeScript sem que haja uma validação real garantindo que o valor é do tipo afirmado.

**Why:** Lição da avaliação overnight — type assertions sem guarda mascaram bugs de tipo em runtime e passam pelo compilador sem aviso. Foi listado como regra explícita no papel de frontend.

**How to apply:** Se for necessário narrow um tipo, usar type guard (`instanceof`, `typeof`, `in`, ou função de guarda com `value is T`) antes do `as`. Se o `as` for inevitável (ex.: cast de `unknown` pós-parse de JSON), documentar com comentário por que é seguro naquele ponto específico.
