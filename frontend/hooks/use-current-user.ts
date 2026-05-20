'use client'

import { useCallback, useEffect, useState } from 'react'
import { getMyProfile, type UserProfile } from '@/lib/user'
import { getToken } from '@/lib/api'

export function useCurrentUser() {
  const [user, setUser] = useState<UserProfile | null>(null)
  const [loading, setLoading] = useState(true)

  const refresh = useCallback(async () => {
    if (!getToken()) {
      setUser(null)
      setLoading(false)
      return
    }
    setLoading(true)
    try {
      const profile = await getMyProfile()
      setUser(profile)
    } catch {
      setUser(null)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    refresh()
  }, [refresh])

  return { user, loading, refresh }
}
