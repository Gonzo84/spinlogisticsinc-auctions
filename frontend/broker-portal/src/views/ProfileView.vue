<script setup lang="ts">
import { useAuth } from '@/composables/useAuth'

const { userName, userEmail, brokerId, companyName, userRoles } = useAuth()
</script>

<template>
  <div>
    <!-- Header -->
    <div class="mb-6">
      <h1 class="text-2xl font-bold text-gray-900">Profile</h1>
      <p class="mt-1 text-sm text-gray-500">
        Your account information
      </p>
    </div>

    <!-- Profile card -->
    <div class="card">
      <div class="flex items-start gap-6">
        <Avatar
          :label="userName.charAt(0).toUpperCase()"
          shape="circle"
          size="xlarge"
          class="bg-primary-100 text-primary-700 shrink-0"
        />
        <div class="flex-1 space-y-4">
          <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div>
              <p class="label">Name</p>
              <p class="text-sm font-medium text-gray-900">{{ userName }}</p>
            </div>
            <div>
              <p class="label">Email</p>
              <p class="text-sm font-medium text-gray-900">{{ userEmail }}</p>
            </div>
            <div>
              <p class="label">Broker ID</p>
              <p class="text-xs font-mono text-gray-600">{{ brokerId }}</p>
            </div>
            <div v-if="companyName">
              <p class="label">Company</p>
              <p class="text-sm font-medium text-gray-900">{{ companyName }}</p>
            </div>
          </div>

          <!-- Roles -->
          <div>
            <p class="label">Roles</p>
            <div class="flex flex-wrap gap-2">
              <Tag
                v-for="role in userRoles"
                :key="role"
                :value="role"
                severity="info"
              />
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
