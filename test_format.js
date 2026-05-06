// Test de formatCurrencyInput
const money = new Intl.NumberFormat("es-CO");

function parseCurrencyInput(value) {
  const normalized = value.replace(/[^\d]/g, "");
  return Number(normalized || "0");
}

function formatCurrencyInput(value) {
  const parsed = parseCurrencyInput(value);
  return parsed > 0 ? money.format(parsed) : "";
}

// Tests
console.log("Test 1 - 200000:");
console.log("  Input: '200000'");
console.log("  Parsed:", parseCurrencyInput("200000"));
console.log("  Formatted:", formatCurrencyInput("200000"));
console.log("  Expected: '200.000'");
console.log("");

console.log("Test 2 - 200.000 (con puntos):");
console.log("  Input: '200.000'");
console.log("  Parsed:", parseCurrencyInput("200.000"));
console.log("  Formatted:", formatCurrencyInput("200.000"));
console.log("  Expected: '200.000'");
console.log("");

console.log("Test 3 - 50000:");
console.log("  Input: '50000'");
console.log("  Formatted:", formatCurrencyInput("50000"));
console.log("  Expected: '50.000'");
