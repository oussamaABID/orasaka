"use client";

import * as React from "react";

interface AnimatedCounterProps {
  value: number;
  suffix?: string;
  duration?: number;
}

/**
 * Animated number counter that smoothly transitions from the current
 * display value to the target value using an ease-out cubic easing curve.
 */
export const AnimatedCounter: React.FC<AnimatedCounterProps> = ({
  value,
  suffix = "",
  duration = 800,
}) => {
  const [display, setDisplay] = React.useState(0);
  const ref = React.useRef<number | null>(null);

  React.useEffect(() => {
    const start = performance.now();
    const from = display;

    const tick = (now: number) => {
      const elapsed = now - start;
      const progress = Math.min(elapsed / duration, 1);
      // ease-out cubic
      const eased = 1 - Math.pow(1 - progress, 3);
      setDisplay(Math.round(from + (value - from) * eased));
      if (progress < 1) {
        ref.current = requestAnimationFrame(tick);
      }
    };

    ref.current = requestAnimationFrame(tick);
    return () => {
      if (ref.current) cancelAnimationFrame(ref.current);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [value, duration]);

  return (
    <span>
      {display}
      {suffix}
    </span>
  );
};
