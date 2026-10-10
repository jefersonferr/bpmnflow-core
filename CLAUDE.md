## Regras de entrega
- Responda em português.
- Nunca edite arquivos em src/ diretamente.
- Para cada lote de tarefas, gere um patch unificado em patches/NNN-lote.patch
  e explique em 3 a 5 linhas o que muda e por quê.
- Artefatos do Spec Kit (specs/, .specify/) podem ser editados diretamente.
- Toda mudança na API pública declara o impacto semver (patch, minor ou major) no plan.md.
- Nunca execute git commit, push, merge ou rebase.
- Altere arquivos somente com as ferramentas Edit/Write, nunca via shell
  (printf, echo >, sed -i, tee, cat <<EOF etc.).
- Não sugira linhas Co-Authored-By nas mensagens de commit.