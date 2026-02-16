type TicketLine = { name: string; qty: number; unitPrice?: number; note?: string };

function openPrint(html: string) {
  const w = window.open("", "_blank", "width=360,height=700");
  if (!w) return;
  w.document.write(html);
  w.document.close();
  w.focus();
  w.onafterprint = () => w.close();
  w.print();
}

export function printInvoice(payload: {
  restaurant: string;
  date: string;
  payment: string;
  items: TicketLine[];
  total: number;
}) {
  const rows = payload.items
    .map((i) => `<tr><td>${i.qty}x ${i.name}</td><td class="r">${(i.unitPrice ?? 0).toFixed(2)}</td></tr>`)
    .join("");

  openPrint(`
    <html>
      <head>
        <style>
          @page { size: 80mm auto; margin: 2mm; }
          html, body { margin: 0; padding: 0; }
          body { font-family: monospace; width: 76mm; font-size: 11px; line-height: 1.2; }
          h3, p { text-align: center; margin: 0; }
          .sep { border-top: 1px dashed #000; margin: 3px 0; }
          table { width: 100%; border-collapse: collapse; }
          td { padding: 1px 0; vertical-align: top; }
          .r { text-align: right; white-space: nowrap; }
          .tot { font-weight: 700; margin-top: 2px; }
        </style>
      </head>
      <body>
        <h3>${payload.restaurant}</h3>
        <p>${payload.date}</p>
        <div class="sep"></div>
        <table>${rows}</table>
        <div class="sep"></div>
        <p class="tot">TOTAL: ${payload.total.toFixed(2)}</p>
        <p>Metodo: ${payload.payment}</p>
      </body>
    </html>
  `);
}

export function printKitchen(payload: { ticketNumber: number; items: TicketLine[] }) {
  const lines = payload.items
    .map((i) => `<div class="it"><b>${i.qty}x ${i.name}</b>${i.note ? `<div>Obs: ${i.note}</div>` : ""}</div>`)
    .join("");

  openPrint(`
    <html>
      <head>
        <style>
          @page { size: 80mm auto; margin: 2mm; }
          html, body { margin: 0; padding: 0; }
          body { font-family: monospace; width: 76mm; font-size: 12px; line-height: 1.2; }
          h3 { text-align: center; margin: 0; }
          .sep { border-top: 1px dashed #000; margin: 3px 0; }
          .it { margin: 2px 0; }
        </style>
      </head>
      <body>
        <h3>TICKET COCINA #${payload.ticketNumber}</h3>
        <div class="sep"></div>
        ${lines}
      </body>
    </html>
  `);
}

