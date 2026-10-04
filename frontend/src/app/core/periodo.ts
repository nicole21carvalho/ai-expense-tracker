/** Primeiro e ultimo dia do mes, em ISO, que e o formato que a API espera. */
export function mesAtual(referencia = new Date()): { inicio: string; fim: string } {
  const ano = referencia.getFullYear();
  const mes = referencia.getMonth();
  // Dia 0 do mes seguinte e o ultimo dia deste. Resolve fevereiro e bissexto
  // sem tabela de dias por mes.
  return {
    inicio: paraIso(new Date(ano, mes, 1)),
    fim: paraIso(new Date(ano, mes + 1, 0))
  };
}

export function rotuloDoMes(referencia = new Date()): string {
  return referencia.toLocaleDateString('pt-BR', { month: 'long', year: 'numeric' });
}

/** toISOString() converte para UTC e pode voltar um dia no fuso do Brasil. */
function paraIso(data: Date): string {
  const mes = String(data.getMonth() + 1).padStart(2, '0');
  const dia = String(data.getDate()).padStart(2, '0');
  return `${data.getFullYear()}-${mes}-${dia}`;
}
