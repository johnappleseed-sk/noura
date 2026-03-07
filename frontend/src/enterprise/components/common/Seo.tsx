import { Helmet } from 'react-helmet-async'
import { env } from '@/config/env'

interface SeoProps {
  title: string
  description: string
}

/**
 * Renders the Seo component.
 *
 * @param param1 The param1 value.
 * @returns The rendered component tree.
 */
export const Seo = ({ title, description }: SeoProps): JSX.Element => (
  <Helmet prioritizeSeoTags>
    <title>{`${title} | ${env.appName}`}</title>
    <meta content={description} name="description" />
    <meta content="index, follow" name="robots" />
  </Helmet>
)
