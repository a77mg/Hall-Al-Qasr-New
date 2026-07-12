import sys

with open('app/src/main/java/com/example/ui/CalendarScreen.kt', 'r') as f:
    lines = f.readlines()

for i, line in enumerate(lines):
    if 'MonthHeader(' in line and 'currentMonth' in lines[i+1]:
        insert_idx = i
        break

code_to_insert = """
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = {
                    val time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
                    exportLauncher.launch("bookings_export_$time.csv")
                }) {
                    Text("تصدير CSV")
                }
                OutlinedButton(onClick = {
                    importLauncher.launch(arrayOf("text/comma-separated-values", "text/csv"))
                }) {
                    Text("استيراد CSV")
                }
            }
"""

lines.insert(insert_idx, code_to_insert)

with open('app/src/main/java/com/example/ui/CalendarScreen.kt', 'w') as f:
    f.writelines(lines)
