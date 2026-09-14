# Especificações

`specs/` contém as especificações de implementação do projeto. Cada `SPEC-XXX-description.md` representa uma unidade planejada de evolução. Implemente as specs em ordem quando houver dependências; uma spec não autoriza funcionalidades de specs futuras.

A prioridade de contexto é:

```text
CLAUDE.md
    ↓
SPEC ativa
    ↓
Código existente
```

`CLAUDE.md` define os princípios globais. A spec ativa define o escopo da implementação atual.
