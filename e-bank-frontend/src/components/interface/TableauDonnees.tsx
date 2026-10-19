import { clsx } from "clsx";

interface ColonneTableau<T> {
  key: string;
  entete: string;
  numerique?: boolean;
  rendu: (ligne: T) => React.ReactNode;
}

// DESIGN.md tables: header 40px tinted uppercase-12px, rows 48px, numerics right + tabular
export function TableauDonnees<T extends { id: number | string }>({
  colonnes,
  lignes,
  messageVide = "Aucune donnée.",
  legende,
}: {
  colonnes: ColonneTableau<T>[];
  lignes: T[];
  messageVide?: string;
  legende?: string;
}) {
  return (
    <div className="overflow-x-auto rounded-lg border border-divider bg-surface-container-lowest">
      <table className="w-full border-collapse">
        {legende && <caption className="sr-only">{legende}</caption>}
        <thead>
          <tr className="bg-surface-tint">
            {colonnes.map((c) => (
              <th
                key={c.key}
                scope="col"
                className={clsx(
                  "h-10 border-b border-divider px-space-md font-label-sm text-label-sm uppercase tracking-wider text-neutral-core",
                  c.numerique ? "text-right" : "text-left"
                )}
              >
                {c.entete}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {lignes.length === 0 ? (
            <tr>
              <td colSpan={colonnes.length} className="h-12 px-space-md text-center font-body-md text-body-md text-neutral-core">
                {messageVide}
              </td>
            </tr>
          ) : (
            lignes.map((ligne) => (
              <tr key={ligne.id} className="h-12 border-b border-divider last:border-0 hover:bg-surface-tint">
                {colonnes.map((c) => (
                  <td
                    key={c.key}
                    className={clsx(
                      "px-space-md font-body-md text-body-md text-on-surface",
                      c.numerique && "text-right tabular-nums"
                    )}
                  >
                    {c.rendu(ligne)}
                  </td>
                ))}
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  );
}

export { TableauDonnees as DataTable, type ColonneTableau as DataTableColumn }
