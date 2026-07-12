#!/bin/bash
sed -i 's/import java.util.\*/import java.util.*\nimport androidx.activity.compose.rememberLauncherForActivityResult\nimport androidx.activity.result.contract.ActivityResultContracts\nimport androidx.compose.ui.platform.LocalContext\nimport java.time.LocalDateTime/g' app/src/main/java/com/example/ui/CalendarScreen.kt

sed -i '/val snackbarHostState = remember { SnackbarHostState() }/a \
    val context = LocalContext.current\n\
    val exportLauncher = rememberLauncherForActivityResult(\n\
        contract = ActivityResultContracts.CreateDocument("text/csv")\n\
    ) { uri ->\n\
        if (uri != null) {\n\
            viewModel.exportBookings(context, uri)\n\
        }\n\
    }\n\
\n\
    val importLauncher = rememberLauncherForActivityResult(\n\
        contract = ActivityResultContracts.OpenDocument()\n\
    ) { uri ->\n\
        if (uri != null) {\n\
            viewModel.importBookings(context, uri)\n\
        }\n\
    }' app/src/main/java/com/example/ui/CalendarScreen.kt

sed -i '/MonthHeader(/i \
            Row(\n\
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),\n\
                horizontalArrangement = Arrangement.SpaceEvenly\n\
            ) {\n\
                Button(onClick = {\n\
                    val time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))\n\
                    exportLauncher.launch("bookings_export_$time.csv")\n\
                }) {\n\
                    Text("تصدير CSV")\n\
                }\n\
                OutlinedButton(onClick = {\n\
                    importLauncher.launch(arrayOf("text/comma-separated-values", "text/csv"))\n\
                }) {\n\
                    Text("استيراد CSV")\n\
                }\n\
            }' app/src/main/java/com/example/ui/CalendarScreen.kt
