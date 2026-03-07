import { axiosClient } from '@/api/axiosClient'
import { extractApiMessage, unwrapApiResponse } from '@/api/backendApi'
import { ApiResponse } from '@/types'

export interface AccountProfileDto {
  id: string
  fullName: string
  email: string
  phone?: string
  roles: string[]
  enabled: boolean
  preferredStoreId?: string | null
}

export interface AddressDto {
  id: string
  label?: string
  fullName: string
  line1: string
  city: string
  state: string
  zipCode: string
  country: string
  defaultAddress: boolean
}

export interface AddressRequest {
  label?: string
  fullName: string
  line1: string
  city: string
  state: string
  zipCode: string
  country: string
  defaultAddress: boolean
}

export interface PaymentMethodDto {
  id: string
  methodType: 'CARD' | 'INVOICE' | 'WALLET'
  provider: string
  tokenizedReference: string
  defaultMethod: boolean
}

export interface PaymentMethodRequest {
  methodType: 'CARD' | 'INVOICE' | 'WALLET'
  provider: string
  tokenizedReference: string
  defaultMethod: boolean
}

export interface CompanyProfileDto {
  id: string
  companyName: string
  taxId?: string
  costCenter?: string
  approvalEmail?: string
  approvalRequired: boolean
  approvalThreshold: number
}

export interface CompanyProfileRequest {
  companyName: string
  taxId?: string
  costCenter?: string
  approvalEmail?: string
  approvalRequired: boolean
  approvalThreshold: number
}

export interface ApprovalDto {
  id: string
  requesterId: string
  orderId?: string | null
  amount: number
  status: 'PENDING' | 'APPROVED' | 'REJECTED'
  reviewerNotes?: string
}

export const accountApi = {
  getProfile: async (): Promise<AccountProfileDto> => {
    try {
      const response = await axiosClient.get<ApiResponse<AccountProfileDto>>('/account/profile')
      return unwrapApiResponse(response.data)
    } catch (error) {
      throw new Error(extractApiMessage(error, 'Unable to load account profile'))
    }
  },

  updateProfile: async (payload: { fullName: string; phone?: string }): Promise<AccountProfileDto> => {
    try {
      const response = await axiosClient.put<ApiResponse<AccountProfileDto>>('/account/profile', payload)
      return unwrapApiResponse(response.data)
    } catch (error) {
      throw new Error(extractApiMessage(error, 'Unable to update account profile'))
    }
  },

  getAddresses: async (): Promise<AddressDto[]> => {
    try {
      const response = await axiosClient.get<ApiResponse<AddressDto[]>>('/account/addresses')
      return unwrapApiResponse(response.data)
    } catch (error) {
      throw new Error(extractApiMessage(error, 'Unable to load addresses'))
    }
  },

  addAddress: async (payload: AddressRequest): Promise<AddressDto> => {
    try {
      const response = await axiosClient.post<ApiResponse<AddressDto>>('/account/addresses', payload)
      return unwrapApiResponse(response.data)
    } catch (error) {
      throw new Error(extractApiMessage(error, 'Unable to add address'))
    }
  },

  getPaymentMethods: async (): Promise<PaymentMethodDto[]> => {
    try {
      const response = await axiosClient.get<ApiResponse<PaymentMethodDto[]>>('/account/payment-methods')
      return unwrapApiResponse(response.data)
    } catch (error) {
      throw new Error(extractApiMessage(error, 'Unable to load payment methods'))
    }
  },

  addPaymentMethod: async (payload: PaymentMethodRequest): Promise<PaymentMethodDto> => {
    try {
      const response = await axiosClient.post<ApiResponse<PaymentMethodDto>>('/account/payment-methods', payload)
      return unwrapApiResponse(response.data)
    } catch (error) {
      throw new Error(extractApiMessage(error, 'Unable to add payment method'))
    }
  },

  upsertCompanyProfile: async (payload: CompanyProfileRequest): Promise<CompanyProfileDto> => {
    try {
      const response = await axiosClient.put<ApiResponse<CompanyProfileDto>>('/account/company-profile', payload)
      return unwrapApiResponse(response.data)
    } catch (error) {
      throw new Error(extractApiMessage(error, 'Unable to save company profile'))
    }
  },

  getApprovals: async (): Promise<ApprovalDto[]> => {
    try {
      const response = await axiosClient.get<ApiResponse<ApprovalDto[]>>('/account/approvals')
      return unwrapApiResponse(response.data)
    } catch (error) {
      throw new Error(extractApiMessage(error, 'Unable to load approvals'))
    }
  },
}
