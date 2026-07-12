package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.models.User
import com.example.models.UserRole
import com.example.models.UserStatus
import com.example.viewmodel.StaffManagementViewModel
import com.example.viewmodel.UiEvent
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffManagementScreen(
    viewModel: StaffManagementViewModel = viewModel(),
    onBack: () -> Unit
) {
    val users by viewModel.users.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()
    
    var showAddDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(key1 = true) {
        viewModel.uiEvent.collectLatest { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is UiEvent.ShowError -> {
                    snackbarHostState.showSnackbar(event.message, withDismissAction = true)
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("إدارة الموظفين") },
                navigationIcon = {
                    Button(onClick = onBack) {
                        Text("عودة")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Text("+")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    if (error != null) {
                        Text(text = error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
                    }
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(users) { user ->
                            UserItemCard(
                                user = user,
                                onToggleStatus = { viewModel.toggleUserStatus(user) }
                            )
                        }
                    }
                }
            }
        }
        if (showAddDialog) {
            AddUserDialog(
                onDismiss = { showAddDialog = false },
                onAdd = { name, email, role ->
                    viewModel.addUser(name, email, role)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun UserItemCard(user: User, onToggleStatus: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = user.name, style = MaterialTheme.typography.titleMedium)
                Text(text = user.email)
                Text(text = "الدور: ${user.role.name}")
                Text(
                    text = "الحالة: ${if (user.status == UserStatus.ACTIVE) "نشط" else "معلق"}",
                    color = if (user.status == UserStatus.ACTIVE) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                )
            }
            Switch(
                checked = user.status == UserStatus.ACTIVE,
                onCheckedChange = { onToggleStatus() }
            )
        }
    }
}

@Composable
fun AddUserDialog(onDismiss: () -> Unit, onAdd: (String, String, UserRole) -> Unit) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(UserRole.Receptionist) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة موظف جديد") },
        text = {
            Column {
                Text("ملاحظة: يجب إنشاء الحساب للمستخدم في Firebase Auth بواسطة الأدمن حتى يتمكن من الدخول.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("الاسم") })
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("البريد الإلكتروني") })
                Spacer(modifier = Modifier.height(8.dp))
                Text("الدور:")
                UserRole.entries.forEach { r ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = role == r, onClick = { role = r })
                        Text(r.name)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onAdd(name, email, role) }) { Text("إضافة") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
