import { useRef, useState } from "react";
import {
  formatCurrencyInput,
  parseCurrencyInput,
  calculateCurrencyInputCursorPosition,
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

  const displayValue = formatCurrencyInput(cleanValue);

  function handleChange(e: React.ChangeEvent<HTMLInputElement>) {
    const inputElement = e.currentTarget;
    const newDisplayValue = e.target.value;
    const oldDisplayValue = displayValue;
    const cursorPos = inputElement.selectionStart || 0;

    const result = handleCurrencyInput(newDisplayValue, options);
    setError(result.error);
    setCleanValue(result.cleanValue);

    const newDisplayFormated = formatCurrencyInput(result.cleanValue);
    const newCursorPos = calculateCurrencyInputCursorPosition(
      cleanValue,
      result.cleanValue,
      cursorPos,
      oldDisplayValue
    );

    setTimeout(() => {
      if (inputRef.current) {
        inputRef.current.setSelectionRange(newCursorPos, newCursorPos);
      }
    }, 0);
  }

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
