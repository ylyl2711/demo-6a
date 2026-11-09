import dashboard from './dashboard';
import warehouse from './warehouse';
import stock from './stock';
import batch from './batch';
import workorder from './workorder';
import quality from './quality';
import system from './system';

export default [
  ...dashboard,
  ...warehouse,
  ...stock,
  ...batch,
  ...workorder,
  ...quality,
  ...system,
];
