import type { CSSProperties } from 'react';
import { FiArrowRight } from 'react-icons/fi';
import type { PromoPanel } from '../../types';
import styles from './PromoBannerGrid.module.css';

interface PromoBannerGridProps {
  panels: PromoPanel[];
}

/**
 * Renders the PromoBannerGrid component.
 *
 * @param param1 The param1 value.
 * @returns The result of promo banner grid.
 */
export function PromoBannerGrid({ panels }: PromoBannerGridProps) {
  return (
    <section className={styles.grid} aria-label="Promotional banners">
      {panels.map((panel) => (
        <article
          key={panel.id}
          className={styles.panel}
          style={{ '--panel-accent': panel.accent } as CSSProperties}
        >
          <div className={styles.header}>
            <h3>{panel.title}</h3>
            {panel.ctaText ? (
              <button type="button" className={styles.cta}>
                {panel.ctaText}
                <FiArrowRight aria-hidden="true" />
              </button>
            ) : null}
          </div>

          <p>{panel.subtitle}</p>

          {panel.priceItems ? (
            <ul className={styles.priceList}>
              {panel.priceItems.map((item) => (
                <li key={item.id}>
                  <span>{item.name}</span>
                  <strong>¥{item.price}</strong>
                </li>
              ))}
            </ul>
          ) : null}
        </article>
      ))}
    </section>
  );
}
