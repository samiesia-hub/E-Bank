"use client";

import { useEffect, useRef } from "react";

type Props = {
  src: string;
  label: string;
  className?: string;
};

// Bandeau vidéo d'accueil : lecture automatique en boucle, sans son,
// sans contrôles. Le useEffect relance la lecture si le navigateur
// l'interrompt (changement d'onglet, économiseur d'énergie, etc.).
export function VideoPresentation({ src, label, className }: Props) {
  const videoRef = useRef<HTMLVideoElement>(null);

  useEffect(() => {
    const video = videoRef.current;
    if (!video) return;

    video.muted = true;
    video.defaultMuted = true;

    const relancer = () => {
      if (video.paused) void video.play().catch(() => {});
    };

    relancer();
    video.addEventListener("loadeddata", relancer);
    video.addEventListener("canplay", relancer);
    document.addEventListener("visibilitychange", relancer);
    window.addEventListener("pointerdown", relancer, { once: true });
    window.addEventListener("touchstart", relancer, { once: true });

    return () => {
      video.removeEventListener("loadeddata", relancer);
      video.removeEventListener("canplay", relancer);
      document.removeEventListener("visibilitychange", relancer);
      window.removeEventListener("pointerdown", relancer);
      window.removeEventListener("touchstart", relancer);
    };
  }, []);

  return (
    <video
      ref={videoRef}
      src={src}
      aria-label={label}
      className={className}
      autoPlay
      muted
      loop
      playsInline
      preload="auto"
    >
      Votre navigateur ne prend pas en charge la lecture vidéo.
    </video>
  );
}
