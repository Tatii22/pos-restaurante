const money = new Intl.NumberFormat("es-CO");

// Test directo
console.log('money.format(200000):', money.format(200000));
console.log('money.format(50000):', money.format(50000));
console.log('money.format(1500):', money.format(1500));
console.log('money.format(123456789):', money.format(123456789));
