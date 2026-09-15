# Especificações

`specs/` contém as especificações de implementação do projeto. Cada `SPEC-XXX-description.md` representa uma unidade planejada de evolução. Implemente as specs em ordem quando houver dependências; uma spec não autoriza funcionalidades de specs futuras.

## Modelo padrão

Novas especificações devem partir de [`SPEC-TEMPLATE.md`](SPEC-TEMPLATE.md) e manter suas seções principais. Uma seção pode declarar “Não se aplica”, mas não deve ser removida. Isso mantém explícitos o escopo, os contratos, os testes, os critérios de aceite e o ponto de parada de cada incremento.

Convenção de nome:

```text
SPEC-XXX-description.md
```

Cada spec deve ser autocontida, verificável e registrar:

- objetivo e resultado esperado;
- dependências e pré-condições;
- escopo e fora do escopo;
- requisitos e decisões técnicas;
- arquivos afetados;
- cenários de teste;
- critérios de aceite e Definition of Done;
- critério de parada.

A prioridade de contexto é:

```text
CLAUDE.md
    ↓
SPEC ativa
    ↓
Código existente
```

`CLAUDE.md` define os princípios globais. A spec ativa define o escopo da implementação atual.

## Índice

| Spec | Título | Estado |
| --- | --- | --- |
| [SPEC-001](SPEC-001-project-bootstrap.md) | Bootstrap e arquitetura inicial | Concluída |
| [SPEC-002](SPEC-002-domain-model.md) | Modelo de domínio inicial | Concluída |
