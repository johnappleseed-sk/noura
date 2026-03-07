import { useDispatch, useSelector } from 'react-redux';
import type { AppDispatch, RootState } from '@/app/store';

export const useAppDispatch = (): AppDispatch => useDispatch<AppDispatch>();
/**
 * Executes use app selector.
 *
 * @param selector The selector value.
 * @returns The result of use app selector.
 */
export const useAppSelector = <TSelected>(selector: (state: RootState) => TSelected): TSelected =>
  useSelector(selector);
