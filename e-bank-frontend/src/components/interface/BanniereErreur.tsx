"use client";

import { AlertTriangle, RefreshCw } from "lucide-react";
import { messageErreurApi } from "@/lib/client-api";

interface Props {
  erreur: unknown;
  reessayer?: () => void;
}

/** Bannière d'erreur réseau sans crash React — critère d'acceptation S2-CLI-05. */
export function BanniereErreur({ erreur, reessayer }: Props) {
  const message = messageErreurApi(erreur);
  return (
    <div
      role="alert"
      className="flex items-start gap-space-md rounded-lg border border-[#f5c6c6] bg-[#fff5f5] p-space-lg"
    >
      <AlertTriangle className="mt-0.5 shrink-0 text-[#c0392b]" size={20} aria-hidden="true" />
      <div className="min-w-0 flex-1">
        <p className="font-label-md text-label-md font-semibold text-[#922b21]">
          Une erreur est survenue
        </p>
        <p className="mt-space-xs font-body-sm text-body-sm text-[#c0392b]">{message}</p>
        {reessayer && (
          <button
            onClick={reessayer}
            className="mt-space-md inline-flex items-center gap-space-xs font-label-sm text-label-sm text-[#922b21] hover:underline"
          >
            <RefreshCw size={14} aria-hidden="true" />
            Réessayer
          </button>
        )}
      </div>
    </div>
  );
}

