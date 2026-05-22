import { useRef, useState, useCallback } from "react";
import {
  formatCurrencyInput,
  parseCurrencyInput,
  handleCurrencyInput,
} from "../utils";

interface UseCurrencyInputOptions {
  maxDigits?: number;
  allowZero?: boolean;
}

export function useCurrencyInput(initialValue = "", options?: UseCurrencyInputOptions) {
  const [cleanValue, setCleanValue] = useState(initialValue);
  const [error, setError] = useState("");
  const inputRef = useRef<HTMLInputElement>(null);
  const selectionRef = useRef<{ start: number; end: number }>({ start: 0, end: 0 });

  const displayValue = formatCurrencyInput(cleanValue);

  const handleChange = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
    const inputElement = e.currentTarget;
    const newValue = e.target.value;
    const cursorPos = inputElement.selectionStart || 0;

    selectionRef.current = {
      start: cursorPos,
      end: inputElement.selectionEnd || cursorPos
    };

    const digitsBeforeCursor = newValue.substring(0, cursorPos).replace(/\D/g, "").length;

    const result = handleCurrencyInput(newValue, options);
    setError(result.error);
    setCleanValue(result.cleanValue);

    requestAnimationFrame(() => {
      if (inputRef.current && result.cleanValue) {
        const newDisplayValue = formatCurrencyInput(result.cleanValue);
        let newCursorPos = 0;
        let digitCount = 0;

        for (let i = 0; i < newDisplayValue.length; i++) {
          if (/\d/.test(newDisplayValue[i])) {
            digitCount++;
            if (digitCount > digitsBeforeCursor) {
              newCursorPos = i;
              break;
            }
          }
        }

        if (newCursorPos === 0) {
          newCursorPos = newDisplayValue.length;
        }

        inputRef.current.setSelectionRange(newCursorPos, newCursorPos);
      }
    });
  }, [options]);

  return {
    inputRef,
    displayValue,
    cleanValue,
    numericValue: parseCurrencyInput(cleanValue),
    error,
    handleChange,
    isValid: !error && parseCurrencyInput(cleanValue) > 0,
    setValue: setCleanValue,
  };
}